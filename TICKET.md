# Les tickets organisés par catégories 

## Projet du futur pour la V1
- [] Systémes de Nodes en développement. Afficher les DataDriven sous la fomre de Node.
- [] Pour la pages Items de Enchantment, pouvoir créer un tags, ou afficher une case "?" quand c'est un tags pas lister dans les presets.
- [] Pour la pages "Non" Combinable pourvoir créer des tags d'enchantment.

## Data Components
Voilà tous les points que j'aimerais te faire remonter :

- Le design system UI n'a pas été respecté : certains composants ont été redéveloppés.

- La colonne de gauche dans Recipes est limitée en hauteur à cause de son aspect grid ; elle devrait avoir sa hauteur de manière autonome. Actuellement, quand il y a plusieurs composants, ils deviennent inaccessibles car ils se retrouvent en bas de l'écran, qui n'est pas scrollable.

- Parfois je dois cocher une case pour afficher un switch : ce n'est pas la meilleure ergonomie, et ce n'est pas très bien proposé.

- Enchantment (voir capture : c:\Users\Hardel\Documents\ShareX\Screenshots\2026-04\java_prFpnyGwY3.png) me propose « key », ce qui est incorrect : cela devrait être le registre d'enchantment ici. Ce problème est générique à plusieurs composants. Il faudrait donc un dropdown et non un text input.

- Surutilisation de la Command Palette alors qu'il faudrait des dropdowns pour plus de simplicité. C'est justifié pour Add Components, mais pas pour les éléments qui devraient être des dropdowns.

- Attribute Modifier : j'ai type, modifier, slot, display. C'est incorrect, le vrai composant est type, id, amount, operation, slot, display. Les composants sont donc à vérifier.

- Quand je rajoute un composant, je le vois s'expand, puis après quelques secondes il se collapse instantanément, sans raison.

- L'interaction avec les inputs text/number est mauvaise. Par exemple, dans le champ texte de Damage, lorsque je veux entrer le chiffre 100 : le 1 fonctionne, mais le curseur se replace devant, ce qui donne 01, puis 001.

- Dans le champ texte, il arrive que si je retire le contenu, « null » s'affiche dans l'input. Si je mets un caractère (par exemple a), il prend des quotes et devient "a". Parfois c'est échappé : par exemple, si j'écris les lettres a puis z, j'obtiens "z\"a\"".

- Je perds aléatoirement le focus quand j'écris dans les inputs texte.

- Je me demande si les schémas sont bons pour certains composants.

- Il faut repenser l'UX intégralement. Le visuel est intéressant mais l'UX est mauvaise : trop de clics et trop de verticalité.

- Je te mets le lien d'un repo d'édition de data-driven : son design est rudimentaire, mais ses choix UX sont intéressants. C'est Misode.

- Il y a probablement d'autres bugs à signaler côté ergonomie, mais la communication client → serveur semble fonctionner sans accrocs.

- Référence de code, C:\Users\Hardel\Desktop\repository\asset_editor\decompiled\voxelio.vsc, ce projet est un repository github donc aucune modification ne doit être produite dedans c'est un readonly.

- Parfois je suis en train de configurer le composants, et il se collapse sans raison et je perd les modifications faite dans le composant.

- Dans enchantment par exemple, je vois par défaut: "key" qui vaut "minecraft:key_1", deux possibilités soit afficher "A définir", soit le premier élément du registre.

- Input texte qui remet au début a chaque fois que je tape un caractére.