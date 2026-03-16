/studio info <player>
/studio role <player> set <role> // AutoComplete avec nos roles
/studio permission <player> concept grant <concept> // AutoComplete avec nos concepts
/studio permission <player> concept revoke <concept> // AutoComplete avec les concepts que posséde ce joueur
/studio permission <player> registry ban <registry> // AutoComplete avec le registre des registre
/studio permission <player> registry pardon <registry> // AutoComplete avec les registres que posséde ce joueur

// Ont va pas l'implémenter pour l'instant. Dans le futur. Ont le retire du code
/studio permission <player> element ban <registry> <element> // AutoComplete avec les éléments du registre
/studio permission <player> element pardon <registry> <element> // AutoComplete avec les éléments du registre que posséde ce joueur
/studio permission <player> element reset <registry> // AutoComplete avec les registres. Reset tout les éléments du registre

Role: Admin, Contributor, None
-> Admin: Can do anything
-> Contributor: Can only edit allowed registries/elements and open Voxel Studio
-> None: Can't do anything, if press F8 for open Studio, the window will not be opened with message in minecraft chat.

If player has role "None" and get a permission grant, he automatically become a "Contributor" with feedback in minecraft chat.
If /studio role <player> set "NONE" is used, the player lost ALL permissions
 
-----

En faisant /studio permission <player> concept grant <concept> il peut voir et accéder au concept et a tout les tabs associés.
Ont peut lui donner les perms des registres associés a ce concept automatiquement.

Enfaute faut attribuer a chaque page le ou les registres néccéssaire pour y accéder. 
- Les Tabs sont grisés si le registre n'est pas attribué au joueur. (Même systems que les composant en somme, juste plus propre et générique ici)

Par exemple "Enchantment" a 7 pages actuellement
- Overview (Avoir le concept)
- Main (Enchantment)
- Find (Tags Enchantment)
- Slots (Enchantment)
- Items (Enchantment)
- Exclusive (Enchantment ou Tags Enchantment)
- Technical (Enchantment ou Tags Enchantment)

Le Switch, Card/InlineCard, InputText, Toggle, Button Counter prenne une liste de Registre et il les faut toutes sinon c'est disabled avec un Tooltip qui indique les permissions nécessaires.
(D'ailleurs les permissions devrait être récupérer durant le splash screen, pour être plus rapide et éviter un temps de chargement supplémentaire.)
J'ai essayer de me connecter avec un deuxiéme compte mais j'ai pas les "Pack" et je vois les concepts, mais je vois aucun élément dans overview.
