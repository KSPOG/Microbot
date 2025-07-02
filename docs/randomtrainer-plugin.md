# Random Trainer Plugin


The Random Trainer plugin selects a skill at random and starts the appropriate microbot plugin.  Currently only mining is implemented while other skills are placeholders.

**Features**

* Customize the delay between random skill selections (in minutes)
* Combat section with goals for Attack, Strength, Defence, Ranged and Magic
* Optionally heal when your hitpoints fall below a configured value
* Works with BreakHandler to idle at a bank three minutes before a break

When Mining is under level 15 the script equips the best available pickaxe from your bank, mines tin and copper evenly in Varrock East, and banks the ore when your inventory is full.

The Random Trainer plugin selects a skill at random and starts the appropriate microbot plugin.  Currently only mining is supported; other skills are placeholders.

Settings include the delay between switching skills (in minutes), combat training goals for each style, and a heal threshold.  The plugin idles at a bank when the BreakHandler indicates a break is about to start.

