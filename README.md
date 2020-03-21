# TransmuteIt

![Spigot Downloads](https://img.shields.io/spiget/downloads/76287?style=flat-square)

Welcome to TransmuteIt, my 2nd plugin!

## Lore
This plugin is inspired by ProjectE's transmutation tablet. I'm a big fan of the EMC system, and how you actually need to transmute the item to be able to transmute more of it. This is essentially just the tablet. No stone or anything. You can customize each EMC value and all that.

The design is to mimic it as much as possible, as a result:

1) You need to transmute an item to be able to transmute it.

## Discrepancies from ProjectE

* EMC calculations aren't smart. You can't just set the EMC value for gold ingot and expect it to auto-calculate gold blocks being ingot times 9. I'm sure it's possible, but I really don't wanna spend the time.
* It's just the transmuting, as long as you have the perms, you can transmute.
* As of now, there's no "Tome of Knowledge" to unlock all the discoveries, maybe I'll add a perm for that.
* Enchantments aren't calculated into the EMC value (yet).
* Potions/Enchanted Books or other items that rely on metadata aren't tracked either. Only names so far.

## Bugs / Feedback / Contribution

I appreciate any and all feedback. Make sure to leave it on the [Issues tab](https://github.com/Chew/TransmuteIt/issues), I also welcome [pull requests](https://github.com/Chew/TransmuteIt/pulls) to help me out!

## Builds

Dev builds are occasionally on my [Jenkins server](https://jenkins.chew.pw/job/TransmuteIt/). I don't do it for every commit, only ones that can be ran on your server without *too* many issues. Safe(st) builds are on the [SpigotMC resource page](https://www.spigotmc.org/resources/transmuteit.76287/). If you do use the builds, make sure to stay as up to date as possible!
