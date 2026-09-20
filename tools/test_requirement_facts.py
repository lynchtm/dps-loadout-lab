# SPDX-License-Identifier: BSD-2-Clause
import importlib.util
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('requirements', ROOT / 'tools/refresh-requirement-facts.py')
requirements = importlib.util.module_from_spec(spec)
spec.loader.exec_module(requirements)


class RequirementExtractionTest(unittest.TestCase):
    def test_upgraded_and_enchanted_descriptions(self):
        self.assertEqual({'hitpoints': 90}, requirements.extract(
            'These upgraded gloves require level 90 [[Hitpoints]] to wear. '
            'Creating them needs 83 Crafting and 70 Smithing.'))
        self.assertEqual({'hitpoints': 75}, requirements.extract(
            'An enchanted bracelet that requires a [[Hitpoints]] of 75 to equip.'))
        self.assertEqual({'ranged': 80}, requirements.extract(
            'Requiring level 80 in Ranged to wield, it is made by combining shards.'))

    def test_all_shared_levels(self):
        self.assertEqual(dict(defence=80, strength=80, ranged=80, magic=80), requirements.extract(
            'Boots require level 80 in Defence, Strength, Ranged, and Magic to wear.'))
        self.assertEqual(dict(attack=42, strength=42, defence=42, hitpoints=42, ranged=42, magic=42, prayer=22), requirements.extract(
            'Players must have 42 Attack, Strength, Defence, Hitpoints, Ranged, and Magic, '
            'along with 22 Prayer to buy and wear them.'))

    def test_quest_words_do_not_hide_equip_levels(self):
        self.assertEqual({'attack': 78}, requirements.extract(
            'Equipping the lance requires 78 Attack as well as completion of the Smithing training.'))
        self.assertEqual(dict(magic=70, strength=60, attack=50), requirements.extract(
            'An upgraded staff requiring 70 Magic, 60 Strength, 50 Attack and completion of a quest to wield.'))

    def test_reversed_and_direct_sentences(self):
        self.assertEqual(dict(attack=70, strength=70), requirements.extract(
            'Level 70 Attack and Strength are required to equip this weapon.'))
        self.assertEqual({'defence': 45}, requirements.extract(
            'Players must have a Defence level of at least 45 to wear this helm.'))

    def test_effects_are_not_requirements(self):
        self.assertEqual(dict(ranged=80, defence=80), requirements.extract(
            'Requires 80 Ranged and Defence to equip, and adds a +1 prayer bonus.'))
        for text in ['Wearing this ring restores 4 Prayer.',
                     'A wearer can die when hit for 15 Hitpoints.',
                     'When worn, this robe adds 3 Prayer.',
                     'It can be equipped and gives 40 Magic attack bonus.']:
            self.assertIsNone(requirements.extract(text), text)

    def test_production_and_tool_usage_are_not_equip_levels(self):
        self.assertIsNone(requirements.extract(
            'The ring requires 90 Magic to create before it can be worn.'))
        self.assertEqual({'attack': 60}, requirements.extract(
            'The tool requires 61 Mining to use and 60 Attack to wield.'))

    def test_missing_is_distinct_from_verified_empty(self):
        self.assertIsNone(requirements.extract('This ring is a boss drop.'))
        self.assertIsNone(requirements.extract('Players do not require 42 Attack to wear this seal.'))
        self.assertEqual({}, requirements.extract('This ring has no requirements to be worn.'))

    def test_skill_template(self):
        self.assertEqual({'magic': 75}, requirements.extract('Requires {{SCP|Magic|75}} to wear.'))


class ShippedRequirementsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.records = json.loads((ROOT / 'src/main/resources/com/loadoutlab/equipment-requirements.json').read_text())

    def test_reported_and_high_end_items(self):
        expected = {
            '31106': {'hitpoints': 90},
            '21791': {'magic': 75}, '21793': {'magic': 75}, '21795': {'magic': 75},
            '26767': {}, '11770': {}, '25258': {},
            '28313': {}, '28307': {}, '28310': {}, '28316': {},
            '19544': {'hitpoints': 75}, '23444': {'hitpoints': 75},
            '26762': {'hitpoints': 75}, '24271': {'defence': 70},
            '22978': {'attack': 78}, '27624': {'magic': 70, 'strength': 60, 'attack': 50},
            '7462': {}, '21295': {}, '6585': {}, '2570': {},
        }
        for ident, expected_levels in expected.items():
            self.assertEqual(expected_levels, self.records.get(ident), ident)

    def test_reviewed_sources_are_auditable(self):
        records = json.loads((ROOT / 'tools/reviewed-equipment-requirements.json').read_text())
        for title, record in records.items():
            self.assertTrue(record.get('note'), title)
            self.assertIsInstance(record['requirements'], dict)
            # The six existing entry-level staves predate revision-based reviews.
            if title not in ['Staff', 'Magic staff', 'Staff of air', 'Staff of water', 'Staff of earth', 'Staff of fire']:
                self.assertGreater(record['revision'], 0, title)
                self.assertTrue(record.get('reviewed'), title)

    def test_priority_combat_pages_have_records_for_every_catalog_variant(self):
        facts = json.loads((ROOT / 'src/main/resources/com/loadoutlab/equipment-facts.json').read_text())
        pages = [
            'Confliction gauntlets', 'Tormented bracelet', 'Tormented bracelet (or)',
            'Imbued Saradomin cape', 'Imbued Guthix cape', 'Imbued Zamorak cape',
            'Imbued Saradomin max cape', 'Infernal cape', 'Infernal max cape',
            'Magus ring', 'Seers ring (i)', 'Ultor ring', 'Venator ring', 'Bellator ring',
            'Ring of suffering (i)', 'Ring of the gods (i)', 'Lightbearer',
            'Amulet of rancour', 'Amulet of rancour (s)', 'Amulet of blood fury',
            'Torva full helm', 'Torva platebody', 'Torva platelegs',
            'Sanguine torva platebody', 'Ancestral robe top', 'Twisted ancestral robe top',
            'Masori body (f)', 'Oathplate chest', 'Radiant oathplate chest',
            'Avernic treads (max)', "Elidinis' ward (f)", "Elidinis' ward (or)",
            "Tumeken's shadow", 'Twisted bow', 'Scythe of Vitur',
            'Holy scythe of vitur', 'Sanguine scythe of vitur', 'Dragon hunter lance',
            'Ancient sceptre', 'Venator bow', 'Bow of Faerdhinen',
            'Neitiznot faceguard', 'Elite void top', 'Elite void robe',
            "Dizana's quiver", "Blessed Dizana's quiver", "Dizana's max cape",
        ]
        for page in pages:
            items = [item for item in facts.values() if page in item['sourcePages']]
            self.assertTrue(items, page)
            for item in items:
                self.assertIn(str(item['id']), self.records, (page, item['id']))

    def test_max_cape_keeps_all_skill_requirements(self):
        levels = self.records['21776']
        self.assertEqual(24, len(levels))
        self.assertEqual(99, levels['sailing'])
        self.assertTrue(all(level == 99 for level in levels.values()))


if __name__ == '__main__':
    unittest.main()
