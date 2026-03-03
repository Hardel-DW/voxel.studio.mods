Modèle d'édition                                                                                                                                                                                                                                                                                                                                      
Lecture seule vs éditable :                                                                                                                                                 - Contenu builtin (namespace minecraft, fichiers .zip/.jar) = read-only, on ne modifie jamais l'original                                                                  
- Contenu dans un pack custom sur le disque = éditable directement

Workflow de modification d'un élément vanilla :
1. L'utilisateur veut modifier minecraft:sharpness (ex: maxLevel 5 → 6)
2. Le moteur détecte que c'est un élément builtin → propose de créer un pack si aucun n'est sélectionné
3. Modal de création de pack : icône PNG, nom, namespace (autocomplete depuis le nom, modifiable)
4. L'override est créé : dans le pack sélectionné, sous le namespace minecraft (car c'est minecraft:sharpness), on copie l'original via les Codecs Minecraft
(sérialisation des données actuelles), puis on applique la modification
5. Résultat : le pack custom contient data/minecraft/enchantment/sharpness.json qui surpasse l'original

Workflow d'ajout d'un nouvel élément :
1. L'utilisateur crée un enchantment
2. Il est ajouté dans le pack actuellement sélectionné, au namespace du pack
3. Ex: pack "MonMod" (namespace monmod) → data/monmod/enchantment/mon_enchant.json

Sélection du pack :
- Dans la title bar globale, un sélecteur de pack (location disque)
- Mojang a une API pour les emplacements de datapacks
- On peut aussi choisir le registre (enchantment, loot_table, recipe...)

Logique manquante

Tags :
- Les enchantments ont des tags associés (#minecraft:in_enchanting_table, #minecraft:tradeable, etc.)
- Il faut la logique parse/compile des tags comme dans breeze — quand on modifie un enchantment, les tags doivent être mis à jour (ajouter/retirer l'entrée dans les      
fichiers tag JSON du pack)

Actions :
- Système d'actions (comme breeze) : chaque modification = une Action (setValue, toggleValueInList, etc.)
- L'action modifie les données dans le pack sélectionné
- L'UI se rafraîchit réactivement en fonction des modifications (les composants reflètent l'état actuel)

En résumé

C'est un système de layering : le registre Minecraft donne la base (read-only), les packs custom ajoutent des overrides par-dessus. Le moteur gère la sérialisation via   
Codecs, les tags via parse/compile, et les modifications via un système d'actions réactif.
