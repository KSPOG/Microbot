# Random Trainer Plugin

The Random Trainer plugin selects a skill at random and then trains it.  Currently only mining is implemented while other skills are placeholders.

**Features**

* Customize the delay between random skill selections (in minutes)
* Combat section with goals for Attack, Strength, Defence, Ranged and Magic
* Optionally heal when your hitpoints fall below a configured value
* Works with BreakHandler to idle at a bank three minutes before a break

When Mining is below level 15 the script equips the best available pickaxe from your bank, mines tin and copper evenly in Varrock East, and banks the ore when your inventory is full.  It will only wield a pickaxe if your Attack level meets the requirement (e.g. 40 for rune, 30 for adamant). Once you have at least level 15 it walks to 2970,3239 and mines iron rocks instead.  When your inventory is full it heads to 3045,3236 and uses the deposit box to bank all ore before returning to the mine.