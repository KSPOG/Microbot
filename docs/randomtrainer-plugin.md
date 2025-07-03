# Random Trainer Plugin

The Random Trainer plugin selects a skill at random and then trains it.  Mining and low level woodcutting are implemented while other skills are placeholders.

**Features**

* Customize the delay between random skill selections (in minutes)
* Combat section with goals for Attack, Strength, Defence, Ranged and Magic
* Optionally heal when your hitpoints fall below a configured value
* Works with BreakHandler to idle at a bank three minutes before a break
* Before each new task the bot deposits the entire inventory so it starts with an empty backpack

When Mining is below level 15 the script equips the best available pickaxe from your bank, mines tin and copper evenly in Varrock East, and banks the ore when your inventory is full.  It will only wield a pickaxe if your Attack level meets the requirement (e.g. 40 for rune, 30 for adamant). Once you have at least level 15 it walks to 2970,3239 and mines iron rocks instead.  When your inventory is full it runs to the nearest bank, deposits all ore, and then returns to the mine. After reaching level 30 the miner travels to 3083,3422 to mine coal. When the inventory is full it runs to the nearest bank, deposits all ore, and returns to mining.  Any uncut gems (sapphire, emerald, ruby, and diamond) found while mining are also deposited.

If Woodcutting is below level 15 the trainer withdraws the best axe your account can use, walks to 3162,3454, and chops normal trees until the inventory is full.  Logs are banked at the nearest bank before returning to chop more trees.
