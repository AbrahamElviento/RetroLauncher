package com.example.data.util

object ArcadeDatabase {
private val arcadeMap = mapOf(
        // Metal Slug Series
        "mslug" to "Metal Slug - Super Vehicle-001",
        "mslug2" to "Metal Slug 2 - Super Vehicle-001/II",
        "mslugx" to "Metal Slug X - Super Vehicle-001",
        "mslug3" to "Metal Slug 3",
        "mslug3a" to "Metal Slug 3 (NGM-2560, earlier)",
        "mslug3h" to "Metal Slug 3 (NGH-2560)",
        "mslug4" to "Metal Slug 4",
        "mslug4h" to "Metal Slug 4 (NGH-2630)",
        "mslug5" to "Metal Slug 5",
        "mslug5h" to "Metal Slug 5 (NGH-2680)",
        "ms4plus" to "Metal Slug 4 Plus (bootleg)",
        "ms5plus" to "Metal Slug 5 Plus (bootleg)",
        "mslug5b" to "Metal Slug 5 (bootleg)",
        "mslug3b6" to "Metal Slug 6 (bootleg of Metal Slug 3)",

        // Street Fighter Series
        "sf1" to "Street Fighter",
        "sf2" to "Street Fighter II: The World Warrior",
        "sf2ce" to "Street Fighter II': Champion Edition",
        "sf2hf" to "Street Fighter II': Hyper Fighting",
        "ssf2" to "Super Street Fighter II: The New Challengers",
        "ssf2t" to "Super Street Fighter II Turbo",
        "sfa" to "Street Fighter Alpha: Warriors' Dreams",
        "sfa2" to "Street Fighter Alpha 2",
        "sfa3" to "Street Fighter Alpha 3",
        "sfiii" to "Street Fighter III: New Generation",
        "sfiii2" to "Street Fighter III 2nd Impact: Giant Attack",
        "sfiii3" to "Street Fighter III 3rd Strike: Fight for the Future",

        // Marvel & Capcom Crossovers
        "xmvsf" to "X-Men Vs. Street Fighter",
        "msh" to "Marvel Super Heroes",
        "mshvsf" to "Marvel Super Heroes Vs. Street Fighter",
        "mvsc" to "Marvel Vs. Capcom: Clash of Super Heroes",
        "xmcota" to "X-Men: Children of the Atom",
        "vampj" to "Vampire: The Night Warriors",
        "vsav" to "Vampire Savior: The Lord of Vampire",
        "dstlk" to "Darkstalkers: The Night Warriors",
        "nwarr" to "Night Warriors: Darkstalkers' Revenge",
        "svc" to "SNK vs. Capcom - SVC Chaos (NGM-2690 ~ NGH-2690)",
        "svcboot" to "SNK vs. Capcom - SVC Chaos (bootleg)",
        "svcplus" to "SNK vs. Capcom - SVC Chaos Plus (bootleg, set 1)",
        "svcplusa" to "SNK vs. Capcom - SVC Chaos Plus (bootleg, set 2)",
        "svcsplus" to "SNK vs. Capcom - SVC Chaos Super Plus (bootleg)",

        // King of Fighters Series
        "kof94" to "The King of Fighters '94",
        "kof95" to "The King of Fighters '95",
        "kof95a" to "The King of Fighters '95 (NGM-084, alt board)",
        "kof95h" to "The King of Fighters '95 (NGH-084)",
        "kof96" to "The King of Fighters '96",
        "kof96a" to "The King of Fighters '96 (bug fix revision)",
        "kof96h" to "The King of Fighters '96 (NGH-214)",
        "kof97" to "The King of Fighters '97",
        "kof97h" to "The King of Fighters '97 (NGH-2320)",
        "kof97k" to "The King of Fighters '97 (Korean release)",
        "kof97oro" to "The King of Fighters '97 Chongchu Jianghu Plus 2003 (bootleg)",
        "kof97pls" to "The King of Fighters '97 Plus (bootleg)",
        "kog" to "King of Gladiator (bootleg of The King of Fighters '97)",
        "kof98" to "The King of Fighters '98 - The Slugfest",
        "kof98a" to "The King of Fighters '98 - The Slugfest (NGM-2420, alt board)",
        "kof98k" to "The King of Fighters '98 - The Slugfest (Korean board, set 1)",
        "kof98ka" to "The King of Fighters '98 - The Slugfest (Korean board, set 2)",
        "kof98h" to "The King of Fighters '98 - The Slugfest (NGH-2420)",
        "kof99" to "The King of Fighters '99 - Millennium Battle",
        "kof99h" to "The King of Fighters '99 - Millennium Battle (NGH-2510)",
        "kof99e" to "The King of Fighters '99 - Millennium Battle (earlier)",
        "kof99k" to "The King of Fighters '99 - Millennium Battle (Korean release)",
        "kof99ka" to "The King of Fighters '99 - Millennium Battle (Korean release, non-encrypted)",
        "kof99p" to "The King of Fighters '99 - Millennium Battle (prototype)",
        "kof2000" to "The King of Fighters 2000",
        "kof2000n" to "The King of Fighters 2000 (not encrypted)",
        "kof2001" to "The King of Fighters 2001",
        "kof2001h" to "The King of Fighters 2001 (NGH-2621)",
        "cthd2003" to "Crouching Tiger Hidden Dragon 2003 (hack of KOF 2001)",
        "ct2k3sp" to "Crouching Tiger Hidden Dragon 2003 Super Plus (hack of KOF 2001)",
        "ct2k3sa" to "Crouching Tiger Hidden Dragon 2003 Super Plus (hack of KOF 2001, alt)",
        "kof2002" to "The King of Fighters 2002 - Challenge to Ultimate Match",
        "kof2002b" to "The King of Fighters 2002 (bootleg)",
        "kf2k2pls" to "The King of Fighters 2002 Plus (bootleg set 1)",
        "kf2k2pla" to "The King of Fighters 2002 Plus (bootleg set 2)",
        "kf2k2mp" to "The King of Fighters 2002 Magic Plus (bootleg)",
        "kf2k2mp2" to "The King of Fighters 2002 Magic Plus II (bootleg)",
        "kof10th" to "The King of Fighters 10th Anniversary (bootleg of KOF 2002)",
        "kf10thep" to "The King of Fighters 10th Anniversary Extra Plus (bootleg of KOF 2002)",
        "kf2k5uni" to "The King of Fighters 10th Anniversary 2005 Unique (bootleg of KOF 2002)",
        "kof2k4se" to "The King of Fighters Special Edition 2004 (bootleg of KOF 2002)",
        "kof2003" to "The King of Fighters 2003",
        "kof2003h" to "The King of Fighters 2003 (NGH-2710)",
        "kf2k3bl" to "The King of Fighters 2003 (bootleg, set 1)",
        "kf2k3bla" to "The King of Fighters 2003 (bootleg, set 2)",
        "kf2k3pl" to "The King of Fighters 2004 Plus / Hero (bootleg of KOF 2003)",
        "kf2k3upl" to "The King of Fighters 2004 Ultra Plus (bootleg of KOF 2003)",

        // Fatal Fury & Samurai Shodown
        "fatfury1" to "Fatal Fury: King of Fighters",
        "fatfury2" to "Fatal Fury 2",
        "fatfury3" to "Fatal Fury 3: Road to the Final Victory",
        "fatfursp" to "Fatal Fury Special / Garou Densetsu Special (set 1)",
        "fatfurspa" to "Fatal Fury Special / Garou Densetsu Special (set 2)",
        "garou" to "Garou: Mark of the Wolves",
        "garouh" to "Garou - Mark of the Wolves (NGM-2530 ~ NGH-2530)",
        "garouha" to "Garou - Mark of the Wolves (NGH-2530)",
        "garoup" to "Garou - Mark of the Wolves (prototype)",
        "garoubl" to "Garou - Mark of the Wolves (bootleg)",
        "rbff1" to "Real Bout Fatal Fury",
        "rbff1a" to "Real Bout Fatal Fury / Real Bout Garou Densetsu (bug fix revision)",
        "rbff1k" to "Real Bout Fatal Fury / Real Bout Garou Densetsu (Korean release)",
        "rbff1ka" to "Real Bout Fatal Fury / Real Bout Garou Densetsu (Korean release, bug fix)",
        "rbffspec" to "Real Bout Fatal Fury Special",
        "rbffspeck" to "Real Bout Fatal Fury Special (Korean release)",
        "rbff2" to "Real Bout Fatal Fury 2 - The Newcomers",
        "rbff2h" to "Real Bout Fatal Fury 2 - The Newcomers (NGH-2400)",
        "rbff2k" to "Real Bout Fatal Fury 2 - The Newcomers (Korean release)",
        "samsho" to "Samurai Shodown",
        "samshoh" to "Samurai Shodown / Samurai Spirits (NGH-045)",
        "samsho2" to "Samurai Shodown II",
        "samsho2k" to "Saulabi Spirits / Jin Saulabi Tu Hon (Korean release of SamSho II, set 1)",
        "samsho2ka" to "Saulabi Spirits / Jin Saulabi Tu Hon (Korean release of SamSho II, set 2)",
        "samsho3" to "Samurai Shodown III",
        "samsho3h" to "Samurai Shodown III / Samurai Spirits (NGH-087)",
        "fswords" to "Fighters Swords (Korean release of Samurai Shodown III)",
        "samsho4" to "Samurai Shodown IV: Amakusa's Revenge",
        "samsho4k" to "Paewang Jeonseol / Legend of a Warrior (Korean censored SamSho IV)",
        "samsho5" to "Samurai Shodown V",
        "samsho5a" to "Samurai Shodown V / Samurai Spirits Zero (set 2)",
        "samsho5h" to "Samurai Shodown V / Samurai Spirits Zero (NGH-2700)",
        "samsho5b" to "Samurai Shodown V / Samurai Spirits Zero (bootleg)",
        "samsh5sp" to "Samurai Shodown V Special",
        "samsh5sph" to "Samurai Shodown V Special (NGH-2720, 2nd release, less censored)",
        "samsh5spho" to "Samurai Shodown V Special (NGH-2720, 1st release, censored)",

        // Classical Arcade & Namco / Nintendo
        "pacman" to "Pac-Man",
        "puckman" to "PuckMan",
        "mspacman" to "Ms. Pac-Man",
        "galaga" to "Galaga",
        "galaxian" to "Galaxian",
        "dkong" to "Donkey Kong",
        "dkongjr" to "Donkey Kong Junior",
        "dkong3" to "Donkey Kong 3",
        "mario" to "Mario Bros.",
        "popeye" to "Popeye",
        "digdug" to "Dig Dug",
        "xevious" to "Xevious",

        // Konami & Capcom Beat 'Em Ups
        "tmnt" to "Teenage Mutant Ninja Turtles",
        "tmnt2" to "Teenage Mutant Ninja Turtles: Turtles in Time",
        "simpsons" to "The Simpsons",
        "xmen" to "X-Men",
        "punisher" to "The Punisher",
        "avsp" to "Alien vs. Predator",
        "dino" to "Cadillacs and Dinosaurs",
        "cshk" to "Cadillacs and Dinosaurs",
        "ffight" to "Final Fight",
        "captaincommando" to "Captain Commando",
        "captcomm" to "Captain Commando",
        "kod" to "The King of Dragons",
        "wof" to "Warriors of Fate",

        // Mortal Kombat & Midway
        "mk" to "Mortal Kombat",
        "mk2" to "Mortal Kombat II",
        "mk3" to "Mortal Kombat 3",
        "umk3" to "Ultimate Mortal Kombat 3",
        "nbajam" to "NBA Jam",
        "nbajamte" to "NBA Jam Tournament Edition",
        "wwfmania" to "WWF WrestleMania",
        "rampage" to "Rampage",
        "ramprg" to "Rampage World Tour",

        // Classic Action & Platformers
        "contra" to "Contra",
        "scontra" to "Super Contra",
        "sunset" to "Sunset Riders",
        "strider" to "Strider",
        "ghouls" to "Ghouls'n Ghosts",
        "gnga" to "Ghosts'n Goblins",
        "bublbobl" to "Bubble Bobble",
        "snowbros" to "Snow Bros. - Nick & Tom",
        "snowbro2" to "Snow Bros. 2 - With New Elves",
        "pstone" to "Power Stone",
        "pstone2" to "Power Stone 2",
        "1941" to "1941: Counter Attack",
        "1942" to "1942",
        "1943" to "1943: The Battle of Midway",
        "1944" to "1944: The Loop Master",
        "19xx" to "19XX: The War Against Destiny",

        // Neo-Geo Systems & Classic SNK Titles
        "neogeo" to "Neo-Geo BIOS",
        "nam1975" to "NAM-1975 (NGM-001 ~ NGH-001)",
        "bstars" to "Baseball Stars Professional (NGM-002)",
        "bstarsh" to "Baseball Stars Professional (NGH-002)",
        "bstars2" to "Baseball Stars 2",
        "tpgolf" to "Top Player's Golf (NGM-003 ~ NGH-003)",
        "mahretsu" to "Mahjong Kyo Retsuden (NGM-004 ~ NGH-004)",
        "maglord" to "Magician Lord (NGM-005)",
        "maglordh" to "Magician Lord (NGH-005)",
        "ridhero" to "Riding Hero (NGM-006 ~ NGH-006)",
        "ridheroh" to "Riding Hero (set 2)",
        "alpham2" to "Alpha Mission II / ASO II - Last Guardian (NGM-007 ~ NGH-007)",
        "alpham2p" to "Alpha Mission II / ASO II - Last Guardian (prototype)",
        "ncombat" to "Ninja Combat (NGM-009)",
        "ncombath" to "Ninja Combat (NGH-009)",
        "cyberlip" to "Cyber-Lip (NGM-010)",
        "superspy" to "The Super Spy (NGM-011 ~ NGH-011)",
        "mutnat" to "Mutation Nation (NGM-014 ~ NGH-014)",
        "kotm" to "King of the Monsters (set 1)",
        "kotmh" to "King of the Monsters (set 2)",
        "kotm2" to "King of the Monsters 2 - The Next Thing (NGM-039 ~ NGH-039)",
        "kotm2a" to "King of the Monsters 2 - The Next Thing (older)",
        "kotm2p" to "King of the Monsters 2 - The Next Thing (prototype)",
        "sengoku" to "Sengoku",
        "sengokuh" to "Sengoku / Sengoku Denshou (NGH-017, US)",
        "sengoku2" to "Sengoku 2 / Sengoku Denshou 2",
        "sengoku3" to "Sengoku 3",
        "sengoku3a" to "Sengoku 3 / Sengoku Densho 2001 (set 2)",
        "burningf" to "Burning Fight (NGM-018 ~ NGH-018)",
        "burningfh" to "Burning Fight (NGH-018, US)",
        "burningfpa" to "Burning Fight (prototype, near final, ver 23.3, 910326)",
        "burningfpb" to "Burning Fight (prototype, newer, V07)",
        "burningfp" to "Burning Fight (prototype, older)",
        "lbowling" to "League Bowling (NGM-019 ~ NGH-019)",
        "gpilots" to "Ghost Pilots (NGM-020 ~ NGH-020)",
        "gpilotsh" to "Ghost Pilots (NGH-020, US)",
        "gpilotsp" to "Ghost Pilots (prototype)",
        "joyjoy" to "Puzzled / Joy Joy Kid (NGM-021 ~ NGH-021)",
        "bjourney" to "Blue's Journey / Raguy (ALM-001 ~ ALH-001)",
        "bjourneyh" to "Blue's Journey / Raguy (ALH-001)",
        "quizdais" to "Quiz Daisousa Sen - The Last Count Down (NGM-023 ~ NGH-023)",
        "quizdaisk" to "Quiz Salibtamjeong - The Last Count Down (Korean localized)",
        "quizdai2" to "Quiz Meitantei Neo & Geo - Quiz Daisousa Sen Part 2",
        "quizkof" to "Quiz King of Fighters (SAM-080 ~ SAH-080)",
        "quizkofk" to "Quiz King of Fighters (Korea)",
        "lresort" to "Last Resort",
        "lresortp" to "Last Resort (prototype)",
        "eightman" to "Eight Man (NGM-025 ~ NGH-025)",
        "minasan" to "Minasan no Okagesamadesu! Dai Sugoroku Taikai",
        "legendos" to "Legend of Success Joe / Ashita no Joe Densetsu",
        "2020bb" to "2020 Super Baseball (set 1)",
        "2020bba" to "2020 Super Baseball (set 2)",
        "2020bbh" to "2020 Super Baseball (set 3)",
        "socbrawl" to "Soccer Brawl (NGM-031)",
        "socbrawlh" to "Soccer Brawl (NGH-031)",
        "roboarmy" to "Robo Army",
        "roboarmya" to "Robo Army (NGM-032 ~ NGH-032)",
        "fbfrenzy" to "Football Frenzy (NGM-034 ~ NGH-034)",
        "bakatono" to "Bakatonosama Mahjong Manyuuki (MOM-002 ~ MOH-002)",
        "crsword" to "Crossed Swords (ALM-002 ~ ALH-002)",
        "trally" to "Thrash Rally (ALM-003 ~ ALH-003)",
        "3countb" to "3 Count Bout / Fire Suplex (NGM-043 ~ NGH-043)",
        "aof" to "Art of Fighting / Ryuuko no Ken (NGM-044 ~ NGH-044)",
        "aof2" to "Art of Fighting 2 / Ryuuko no Ken 2 (NGM-056)",
        "aof2a" to "Art of Fighting 2 / Ryuuko no Ken 2 (NGH-056)",
        "aof3" to "Art of Fighting 3 - The Path of the Warrior",
        "aof3k" to "Art of Fighting 3 - The Path of the Warrior (Korean release)",
        "tophunter" to "Top Hunter: Roddy & Cathy",
        "tophuntr" to "Top Hunter - Roddy & Cathy (NGM-046)",
        "tophuntrh" to "Top Hunter - Roddy & Cathy (NGH-046)",
        "janshin" to "Janshin Densetsu - Quest of Jongmaster",
        "androdun" to "Andro Dunos (NGM-049 ~ NGH-049)",
        "ncommand" to "Ninja Commando",
        "viewpoin" to "Viewpoint",
        "viewpoinp" to "Viewpoint (prototype)",
        "ssideki" to "Super Sidekicks / Tokuten Ou",
        "ssideki2" to "Super Sidekicks 2 - The World Championship",
        "ssideki3" to "Super Sidekicks 3 - The Next Glory",
        "ssideki4" to "The Ultimate 11 - The SNK Football Championship",
        "wh1" to "World Heroes (ALM-005)",
        "wh1h" to "World Heroes (ALH-005)",
        "wh1ha" to "World Heroes (set 3)",
        "wh2" to "World Heroes 2 (ALM-006 ~ ALH-006)",
        "wh2h" to "World Heroes 2 (ALH-006)",
        "wh2j" to "World Heroes 2 Jet (ADM-007 ~ ADH-007)",
        "whp" to "World Heroes Perfect",
        "savagere" to "Savage Reign / Fu'un Mokushiroku - Kakutou Sousei",
        "fightfev" to "Fight Fever / Wang Jung Wang (set 1)",
        "fightfeva" to "Fight Fever / Wang Jung Wang (set 2)",
        "spinmast" to "Spin Master",
        "wjammers" to "Windjammers / Flying Power Disc",
        "windj" to "Windjammers / Flying Power Disc",
        "karnovr" to "Karnov's Revenge / Fighter's History Dynamite",
        "gururin" to "Gururin",
        "pspikes2" to "Power Spikes II (NGM-068)",
        "zupapa" to "Zupapa!",
        "panicbom" to "Panic Bomber",
        "aodk" to "Aggressors of Dark Kombat / Tsuukai GANGAN Koushinkyoku",
        "sonicwi2" to "Aero Fighters 2 / Sonic Wings 2",
        "sonicwi3" to "Aero Fighters 3 / Sonic Wings 3",
        "zedblade" to "Zed Blade / Operation Ragnarok",
        "galaxyfg" to "Galaxy Fight - Universal Warriors",
        "strhoop" to "Street Hoop / Street Slam / Dunk Dream",
        "doubledr" to "Double Dragon (Neo-Geo)",
        "pbobble" to "Puzzle Bobble / Bust-A-Move",
        "puzzledp" to "Puzzle Bobble / Bust-A-Move",
        "pbobblen" to "Puzzle Bobble / Bust-A-Move (Neo-Geo, NGM-083)",
        "pbobblenb" to "Puzzle Bobble / Bust-A-Move (Neo-Geo, bootleg)",
        "puzzldpr" to "Puzzle De Pon! R!",
        "pbobble2" to "Puzzle Bobble 2 / Bust-A-Move Again",
        "pbobbl2n" to "Puzzle Bobble 2 / Bust-A-Move Again (Neo-Geo)",
        "twsoc96" to "Tecmo World Soccer '96",
        "stakwin" to "Stakes Winner / Stakes Winner - GI Kinzen Seiha e no Michi",
        "stakwindev" to "Stakes Winner (early development board)",
        "stakwin2" to "Stakes Winner 2",
        "pulstar" to "Pulstar",
        "kabukikl" to "Far East of Eden - Kabuki Klash / Tengai Makyou - Shin Den",
        "neobomu" to "Neo Bomberman",
        "neobombe" to "Neo Bomberman",
        "gowcaizr" to "Voltage Fighter - Gowcaizer",
        "turfmast" to "Neo Turf Masters / Big Tournament Golf",
        "moshougi" to "Shougi no Tatsujin - Master of Shougi",
        "marukodq" to "Chibi Maruko-chan: Maruko Deluxe Quiz",
        "neomrdo" to "Neo Mr. Do!",
        "sdodgeb" to "Super Dodge Ball / Kunio no Nekketsu Toukyuu Densetsu",
        "goalx3" to "Goal! Goal! Goal!",
        "overtop" to "Over Top",
        "neodrift" to "Neo Drift Out - New Technology",
        "kizuna" to "Kizuna Encounter - Super Tag Battle",
        "ninjamas" to "Ninja Master's - Haoh-ninpo-cho",
        "ragnagrd" to "Ragnagard / Shin-Oh-Ken",
        "pgoal" to "Pleasure Goal / Futsal - 5 on 5 Mini Soccer (NGM-219)",
        "ironclad" to "Choutetsu Brikin'ger / Iron Clad (prototype)",
        "ironclado" to "Choutetsu Brikin'ger / Iron Clad (prototype, bootleg)",
        "magdrop2" to "Magical Drop II",
        "magdrop3" to "Magical Drop III",
        "twinspri" to "Twinkle Star Sprites",
        "wakuwaku" to "Waku Waku 7",
        "wakuwak7" to "Waku Waku 7",
        "ghostlop" to "Ghostlop (prototype)",
        "breakers" to "Breakers",
        "breakrev" to "Breakers Revenge",
        "miexchng" to "Money Puzzle Exchanger / Money Idol Exchanger",
        "lastblad" to "The Last Blade / Bakumatsu Roman - Gekka no Kenshi",
        "lastbladh" to "The Last Blade (NGH-2340)",
        "lastsold" to "The Last Soldier (Korean release of The Last Blade)",
        "lastbld2" to "The Last Blade 2 / Bakumatsu Roman - Dai Ni Maku Gekka no Kenshi",
        "irrmaze" to "The Irritating Maze / Ultra Denryu Iraira Bou",
        "popbounc" to "Pop 'n Bounce / Gapporin",
        "shocktro" to "Shock Troopers",
        "shocktroa" to "Shock Troopers (set 2)",
        "shocktr2" to "Shock Troopers: 2nd Squad",
        "blazstar" to "Blazing Star",
        "neocup98" to "Neo-Geo Cup '98 - The Road to the Victory",
        "flipshot" to "Flip Shot",
        "b2b" to "Bang Bang Busters",
        "ctomaday" to "Captain Tomaday",
        "ganryu" to "Ganryu / Musashi Ganryuki",
        "s1945p" to "Strikers 1945 Plus",
        "preisle2" to "Prehistoric Isle 2",
        "bangbead" to "Bang Bead",
        "nitd" to "Nightmare in the Dark",
        "nitdbl" to "Nightmare in the Dark (bootleg)",
        "rotd" to "Rage of the Dragons (NGM-2640?)",
        "rotdh" to "Rage of the Dragons (NGH-2640?)",
        "matrim" to "Matrimelee / Shin Gouketsuji Ichizoku Toukon",
        "matrimbl" to "Matrimelee / Shin Gouketsuji Ichizoku Toukon (bootleg)",
        "pnyaa" to "Pochi and Nyaa (Ver 2.02)",
        "pnyaaa" to "Pochi and Nyaa (Ver 2.00)",
        "jockeygp" to "Jockey Grand Prix (set 1)",
        "jockeygpa" to "Jockey Grand Prix (set 2)",
        "dragonsh" to "Dragon's Heaven (development board)",
        "zintrckb" to "Zintrick / Oshidashi Zentrix (bootleg of CD version)",
        "froman2b" to "Idol Mahjong Final Romance 2 (Neo-Geo, bootleg of CD version)",
        "crswd2bl" to "Crossed Swords 2 (bootleg of CD version)",
        "lans2004" to "Lansquenet 2004 (bootleg of Shock Troopers - 2nd Squad)",
        "lasthope" to "Last Hope",

        // Homebrew, Prototypes & Modern Releases
        "sbp" to "Super Bubble Pop (MVS)",
        "diggerma" to "Digger Man (prototype)",
        "19yy" to "19YY - Ichikyo Wai Wai",
        "baddudes" to "Bad Dudes - Burger Edition (20250628)",
        "bbb2" to "Bang Bang Busters 2 (demo v2.0)",
        "bbb2_dm1" to "Bang Bang Busters 2 (demo v1.0)",
        "cpbarrel" to "Captain Barrel",
        "cybforce" to "Cyborg Force",
        "ddragon1" to "Double Dragon One (beta 3, 20250916)",
        "ddragon1_b2" to "Double Dragon One (beta 2, 20250903)",
        "ddragon1_dm" to "Double Dragon One (demo 20250217)",
        "ddragon1_p2" to "Double Dragon One (prototype 2)",
        "ddragon1_p1" to "Double Dragon One (prototype 1)",
        "etyphoon" to "The Eye of Typhoon (Tsunami Edition, beta 7)",
        "etyphoon_b6" to "The Eye of Typhoon (Tsunami Edition, beta 6)",
        "etyphoon_b5" to "The Eye of Typhoon (Tsunami Edition, beta 5)",
        "etyphoon_b4" to "The Eye of Typhoon (Tsunami Edition, beta 4)",
        "etyphoon_b3" to "The Eye of Typhoon (Tsunami Edition, beta 3)",
        "etyphoon_b2" to "The Eye of Typhoon (Tsunami Edition, beta 2)",
        "etyphoon_b1" to "The Eye of Typhoon (Tsunami Edition, beta 1)",
        "etyphoon_a" to "The Eye of Typhoon (alpha)",
        "gladmort" to "GladMort (demo²)",
        "gladmort_d1" to "GladMort (demo)",
        "goldnaxe" to "Golden Axe",
        "hypernoid" to "Hypernoid",
        "inthunt" to "In The Hunt (demo 20250518)",
        "totc" to "Treasures of The Caribbean",
        "looptris" to "Looptris",
        "looptrsp" to "Looptris Plus",
        "neotris" to "NeoTRIS",
        "nblktiger" to "NeoBlack Tiger (demo)",
        "violentv" to "Violent Vengeance (beta 3.28)",
        "violentv_b2" to "Violent Vengeance (beta 2.05)",
        "violentv_b1" to "Violent Vengeance (beta 1.04)",
        "xenocris" to "Xeno Crisis"
    )

    private var cachedCustomDbPath: String = ""
    private var cachedCustomDbLastModified: Long = 0L
    private val customArcadeMap = mutableMapOf<String, String>()

    private fun loadCustomDatabaseIfNeeded(customDbPath: String) {
        if (customDbPath.isBlank()) {
            customArcadeMap.clear()
            cachedCustomDbPath = ""
            return
        }
        val file = java.io.File(customDbPath)
        if (!file.exists() || !file.canRead()) return

        val lastMod = file.lastModified()
        if (cachedCustomDbPath == customDbPath && cachedCustomDbLastModified == lastMod) {
            return
        }

        customArcadeMap.clear()
        cachedCustomDbPath = customDbPath
        cachedCustomDbLastModified = lastMod

        try {
            val content = file.readText()
            if (content.contains("<game") || content.contains("<machine") || content.contains("<item") || content.contains("<?xml")) {
                val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(java.io.StringReader(content))

                var eventType = parser.eventType
                var currentGameName = ""
                var currentDescription = ""
                var currentTag = ""
                var inSoftware = false
                var inSubElement = false

                while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        org.xmlpull.v1.XmlPullParser.START_TAG -> {
                            currentTag = parser.name.lowercase()
                            if (currentTag == "game" || currentTag == "machine" || currentTag == "software") {
                                currentGameName = parser.getAttributeValue("", "name") ?: parser.getAttributeValue("", "id") ?: ""
                                currentDescription = ""
                                inSoftware = true
                                inSubElement = false
                            } else if (currentTag == "part" || currentTag == "rom" || currentTag == "disk" || currentTag == "info") {
                                inSubElement = true
                            } else if (currentTag == "item") {
                                val name = parser.getAttributeValue("", "name") ?: ""
                                val title = parser.getAttributeValue("", "description") ?: parser.getAttributeValue("", "title") ?: ""
                                if (name.isNotBlank() && title.isNotBlank()) {
                                    val clean = name.lowercase().replace(Regex("[_-]"), "")
                                    customArcadeMap[clean] = title
                                    customArcadeMap[name.lowercase()] = title
                                }
                            }
                        }
                        org.xmlpull.v1.XmlPullParser.TEXT -> {
                            val text = parser.text
                            if (inSoftware && !inSubElement && (currentTag == "description" || currentTag == "title")) {
                                currentDescription += text
                            }
                        }
                        org.xmlpull.v1.XmlPullParser.END_TAG -> {
                            val tag = parser.name.lowercase()
                            if (tag == "game" || tag == "machine" || tag == "software") {
                                val trimmedDesc = currentDescription.trim()
                                if (currentGameName.isNotBlank() && trimmedDesc.isNotBlank()) {
                                    val clean = currentGameName.lowercase().replace(Regex("[_-]"), "")
                                    customArcadeMap[clean] = trimmedDesc
                                    customArcadeMap[currentGameName.lowercase()] = trimmedDesc
                                }
                                currentGameName = ""
                                currentDescription = ""
                                inSoftware = false
                            } else if (tag == "part" || tag == "rom" || tag == "disk" || tag == "info") {
                                inSubElement = false
                            }
                            currentTag = ""
                        }
                    }
                    eventType = parser.next()
                }
            } else {
                var currentName = ""
                content.lineSequence().forEach { rawLine ->
                    val line = rawLine.trim()
                    if (line.startsWith("name ")) {
                        currentName = line.removePrefix("name").trim().removeSurrounding("\"")
                    } else if (line.startsWith("description ") && currentName.isNotBlank()) {
                        val desc = line.removePrefix("description").trim().removeSurrounding("\"")
                        if (desc.isNotBlank()) {
                            val clean = currentName.lowercase().replace(Regex("[_-]"), "")
                            customArcadeMap[clean] = desc
                        }
                        currentName = ""
                    } else if (line.contains("=") || line.contains("|") || line.contains(":")) {
                        val delim = when {
                            line.contains("=") -> "="
                            line.contains("|") -> "|"
                            else -> ":"
                        }
                        val parts = line.split(delim, limit = 2)
                        if (parts.size == 2) {
                            val name = parts[0].trim().removeSurrounding("\"")
                            val title = parts[1].trim().removeSurrounding("\"")
                            if (name.isNotBlank() && title.isNotBlank()) {
                                val clean = name.lowercase().replace(Regex("[_-]"), "")
                                customArcadeMap[clean] = title
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getArcadeTitle(fileNameWithoutExt: String, customDbPath: String = ""): String {
        val cleanStem = fileNameWithoutExt
            .lowercase()
            .replace(Regex("\\(.*\\)|\\[.*\\]"), "")
            .trim()
            .replace("_", "")
            .replace("-", "")

        if (customDbPath.isNotBlank()) {
            loadCustomDatabaseIfNeeded(customDbPath)
            customArcadeMap[cleanStem]?.let { return it }
            val baseStem = cleanStem.replace(Regex("(u|j|e|us|jp|eu|a|b|c)$"), "")
            customArcadeMap[baseStem]?.let { return it }
        }

        // 1. Direct lookup
        arcadeMap[cleanStem]?.let { return it }

        // 2. Lookup with trimmed trailing region / set letters (e.g. mslugu, sf2us, sf2j)
        val baseStem = cleanStem.replace(Regex("(u|j|e|us|jp|eu|a|b|c)$"), "")
        arcadeMap[baseStem]?.let { return it }

        // 3. Fallback: nicely format stem
        return fileNameWithoutExt
            .replace("_", " ")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
    }
}
