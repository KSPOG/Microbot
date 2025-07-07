# Random Trainer Plugin

The Random Trainer plugin selects a skill at random and then trains it.  Mining and low level woodcutting are implemented while other skills are placeholders.

**Features**

* Customize the delay between random skill selections **in minutes**
* Combat section with goals for Attack, Strength, Defence, Ranged and Magic
* Optionally heal when your hitpoints fall below a configured "Heal at HP" value
* Works with BreakHandler to idle at a bank three minutes before a break
* Before each new task the bot deposits the entire inventory so it starts with an empty backpack
* Uses antiban features like contextual variability and dynamic activity

When Mining is below level 15 the script equips the best available pickaxe from your bank, mines tin and copper evenly in Varrock East, and banks the ore when your inventory is full.  It will only wield a pickaxe if your Attack level meets the requirement (e.g. 40 for rune, 30 for adamant). Once you have at least level 15 it walks to 2970,3239 and mines iron rocks instead.  When your inventory is full it uses the nearest deposit box to store everything except your pickaxe before returning to the mine. After reaching level 30 the miner travels to 3083,3422 to mine coal. When the inventory is full it runs to the nearest bank, deposits all ore, and returns to mining.  Any uncut gems (sapphire, emerald, ruby, and diamond) found while mining are also deposited.

If Woodcutting is below level 15 the trainer withdraws the best axe your account can use and heads to 3162,3454 to chop normal trees. When that area is crowded (more than three other players nearby) it instead uses an alternate tree patch at 3276,3444. Logs are banked at the nearest bank before returning to chop more trees. Once Woodcutting reaches level 15 it walks to 3192,3461 and chops oak trees until level 30, banking logs whenever the inventory fills. Between levels 30 and 60 it travels to 3060,3254 and chops willow trees. When the inventory is full it walks to the deposit box at 3046,3235 to store all willow logs before returning.


When Fishing is below level 20 the trainer withdraws a small fishing net from your bank and heads to 3244,3154 to fish shrimp and anchovies. Once the inventory is full it walks upstairs in Lumbridge to the bank chest at 3209,3220,2 and deposits all raw shrimp and raw anchovies before resuming. After reaching level 20 it withdraws a fly fishing rod and feathers, then travels to 3104,3431 to lure fish. When the inventory becomes full it banks the fish at the nearest bank while keeping the rod and feathers.