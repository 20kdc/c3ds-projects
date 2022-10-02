# ciesetup - The ultimate workarounds to fix an ancient game
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

class DirectoryType():
	def __init__(self, a, b):
		self.key = a
		self.filename = b

directory_types = [
	DirectoryType("Main", "."),
	DirectoryType("Sounds", "Sounds"),
	DirectoryType("Images", "Images"),
	DirectoryType("Genetics", "Genetics"),
	DirectoryType("Body Data", "Body Data"),
	DirectoryType("Overlay Data", "Overlay Data"),
	DirectoryType("Backgrounds", "Backgrounds"),
	DirectoryType("Catalogue", "Catalogue"),
	DirectoryType("Bootstrap", "Bootstrap"),
	DirectoryType("Worlds", "My Worlds"),
	DirectoryType("Exported Creatures", "My Creatures"),
	DirectoryType("Resource Files", "My Agents"),
	DirectoryType("Journal", "Journal"),
	DirectoryType("Creature Database", "Creature Galleries"),
	DirectoryType("Users", "Users")
]

class Config():
	def __init__(self):
		self.content = ""

	def add_kv(self, k, v):
		# luckily we never need escaping in these
		self.content += "\"" + k + "\" \"" + v + "\"\n"

class DirectoryManager(Config):
	def __init__(self):
		super().__init__()
		self.counters = {}
		for v in directory_types:
			self.counters[v.key] = 0

	def add_dir(self, k, v):
		if self.counters[k] == 0:
			self.add_kv(k + " Directory", v)
		else:
			self.add_kv("Auxiliary " + str(self.counters[k]) + " " + k + " Directory", v)
		self.counters[k] += 1

	def add_all_dirs(self, at):
		for v in directory_types:
			self.add_dir(v.key, at + v.filename)

