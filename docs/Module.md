# Client 
Le client ne doit qu'afficher et interagir avec les éléments, il ne doit pas avoir de logiques d'écriture, de lecture ou de logiques des datadriven. Il ne doit qu'afficher et réagir.

### Module JavaFX -> Gère tout le rendu.

### Module Debug
Tous ce qui concerne l'avant démarrage de Studio ont utilise le Logger Minecraft, faut bien pouvoir debug le démarrage si elle se lance pas.

Il faut travailler une sorte de React-Scan, un overlay qui affiche le FPS, et indique quels composant est re-rendu avec un encadrés, ont doit le faire le plus dynmaiquement et génériquement possible, une couche par dessus JavaFX uniquement.

Accéssible en bas a droite de la fenêtre, dans la barre latérale.
-Permet d'accéder a un Layout de debug qui contient le Header/Tabs comme concept. (Le même)
- Page Workspace -> Affiche toutes les données et l'état du workspace a l'instant T.
- Page Logs -> Affiche les logs de l'application, ont passe par une API génériques non liés a JavaFX, ont s'en sers de logs a travers l'application. (N'inclut pas Network)
- Page Network -> Affiche les logs de réseau. Ont se place directement a l'endroit ou les requétes client/server parte, et ont fait au plus génériques possibles en filtrant par notre namespace. I18n avec clefs de traduction par concatenation dynamiques par l'identifier, avec title et description.
- Page Render -> Affiche le rendu de l'atlas des items.
- Layers (Mais j'avoue ne pas trop savoir comment faire un truc comme ça, c'est juste une idée, faut voir si c'est possible)


### Module Rendering
Pour obtenir le rendu des items on passe par un module indépendant non lié à rien d'autre mentionné dans ce document.
On crée un atlas supplémentaire par le thread qu'utilise Minecraft pour générer ces atlas et on crée le rendu 2D des items, on utilise le même système que Minecraft pour générer ces atlas et les positions.
Qui sera ensuite utilisé pour afficher un item par son id.

### Module Selector -> Il doit se comporter comme Zustand réagir à des Stores et uniquement re-render les valeurs dans des composants de la manière la plus générique possible sans faire aucun cas par cas. On crée des sélecteurs on s'abonne à des valeurs. Sa conception ne doit pas être liée à i18n, JavaFX (Du moins le moins possible), ou au concept. Il est indépendant.

### Module I18n StudioText.java -> Gère absolument toutes les clefs de trad dynamiques et la logique purement générique tout doit passer par lui. Aucun cas par cas. Les composants prennent string, les routes utilisent StudioText et ce sont eux qui fournissent le contexte ou la clef.

### JavaFX
- VoxelColors.java -> Toutes les couleurs sous la forme de constantes.
- VoxelFonts.java -> Toutes les fonts sont gérées là
- VoxelStudioWindow.java -> Gère la fenêtre et c'est le seul fichier à s'occuper de ça ne doit contenir aucune autre logique. Affichage, Resize, Cursor, Maximize, Minimize, snap, drag.
- Le dossier lib/data, des listes, hashmap qui servent uniquement au rendu des pages des données arbitrairement choisies à but de rendu et d'ergonomie. Le serveur n'en a pas besoin il reçoit juste "Je veux éditer cet élément avec telle action".
- Le dossier routes, ne doit contenir aucun composant générique que les pages, pas de logiques, les clefs de trad sont ici.
- Le dossier components/ui, ce sont des composants ultra génériques non liés à aucune logique, aucun comportement d'aucun concept, pur et 100% générique, pas de I18n dedans.

### Workspace Client State -> Mémoire
Il contient le pack sélectionné, l'état affiché courant, les erreurs/warnings UI, les actions en attente si on veut les stocker

### Gateway -> Comportement
Toutes les interactions passent par une Gateway ultra générique qui envoie au serveur. C'est le seul et unique point d'entrée et de sortie des interactions/actions. Validation locale, Optimistic et Rollback, Le serveur si c'est bon broadcast à tous les autres clients (admin/contributeur), soit rollback, soit valide, les sélecteurs feront la mise à jour des affichages.
Le client dit **"applique cette action sur packA"** et le serveur fait "je prends le workspace de packA, j'applique, je sauvegarde, et je te réponds".

### Pas de vérification côté client d'une logique serveur, par exemple vérifier qu'un tag existe c'est une logique d'écriture et de lecture, nous on se contente d'envoyer le tag, c'est au serveur de se débrouiller, il crée un tag s'il n'existe pas ou l'override s'il existe déjà. On se contente de gérer l'affichage à partir des Registres et de nos données.

### Si on a besoin de données spécifiques de différence c'est au serveur de nous les fournir, c'est lui qui gère la diff, l'écriture et lecture.

------------------------------------------------

# Server
Le serveur reste autoritaire.

# Module Network (Dossier Network)
Ne doit que envoyer et recevoir les données entre le client et le serveur. Ensuite le client/server dispatch et utilise les bonnes classes et modules. Mais globalement il doit juste envoiyer et recevoir les données.

### Module Workspace
le workspace vit côté serveur. état métier par pack

### Module Permissions complètement indépendant de tous les modules, indépendant de JavaFX de la window des concepts, pas lié au système de OP, il permet de définir entre admin, none et Contributor envoyé au Client.
- **Admin**: full access, can promote/demote other players via `/studio role set`.
- **Contributor**: full editing access to all concepts and registries.
- **None**: cannot open Voxel Studio. F8 is blocked with a chat message.

### Module d'écriture/lecture.
Déterminer si l'élément vient de notre pack ou ailleurs, il renvoie une donnée générique.
Pour les Tags, on ne fait pas que de l'override, si le tag n'existe pas on le crée, on peut le supprimer aussi ou le modifier. 

### Sous modules d'écriture/lecture.
- **Lecture Minecraft** - Registry Reader
- **Calcul de diff** - Diff Planner déterminer ce qu'il faut écrire ou supprimer.
- **Écriture disque** - écrit réellement les fichiers Il renvoie erreurs, réussite. Et c'est tout il ne gère que l'écriture.
 à séparer en trois sous modules avec les états communs.

### Module Tags 
Ce mod est un contenu distinct et utilisé dans notre app, les tags et la clef "exclude" (List Identifier ou TagEntry) permet d'exclure des éléments d'un tag, de plus la clef values n'est plus obligatoire. L'exclusion s'applique après la fusion de toutes les valeurs.


----------------- RÈGLE COMMUNE -----------------

Aucune logique Java ne doit dépendre implicitement du code web, le web n'est qu'une inspiration une aide il a une logique et finalité différente donc un comportement différent.  

On doit éviter de dupliquer des sources de vérité et des logiques la datagen est par exemple des deux côtés, le client gère trop de logiques d'enchantment 

C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\lib\action\EnchantmentActions.java. Ce fichier ne devrait pas exister. Les actions et interactions doivent être généralisées et optimistic de manière générale pas au cas par cas pour chaque action.

Le serveur broadcast ou rollback ce qui met à jour les sélecteurs et met à jour l'UI, et les sélecteurs évitent les re-rendu si l'état final est identique.

- Éviter les plusieurs sources d'informations