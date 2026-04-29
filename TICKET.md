# Les tickets organisés par catégories 

## Projet du futur pour la V1
- [] Pour la pages Items de Enchantment, pouvoir créer un tags, ou afficher une case "?" quand c'est un tags pas lister dans les presets.
- [] Pour la pages "Non" Combinable pourvoir créer des tags d'enchantment.

## Structure
- [] Refaire remlacement ont veut pas deux champ et un bouton, mais un clique sur un élément de la listes des blocs et avoir un "Rechercher", "Remplacer".
- [] Rechercher indique tout les endroits ou le bloc apparait avec un clique qui permet de mettre la camera dessus.
- [] Rechercher par NBT un simple regex je pense qui cherche parmis tout les blocs cibler.
- [] Idéalement cliquer sur un blocs que pour les piéces de structure, sinon sa serait peut être trop lourd pour la grosse structure. dans la scéne et voir ces données et pouvoir les modifiers, et calculer a la voler a partir de la position dans la scéne pour ne pas saturer la mémoire.

## Editeur MCDOC
- [x] Systémes de bordure rouge pour les champs requis non défini avec tooltip pour expliquer ce qu'il faut faire.
- [x] Vérifier Unbreakable et le faire fonctionner coté serveur.
- [x] Refaire le visuel du composant qui ont seulement un "Flag present when component is set" et les mettre en inline dans la row et empêcher qu'il soit expandable.
- [x] Refaire le système de tabs pour que l'éditeur de code ne soit pas considéré comme un vrai tabs et que si il n'y a qu'un seul vrai tabs il ne s'affiche pas. 
- [x] Si ont est dans l'éditeeur ont reclique sur l'icone pour revenir a la page d'avant.
- [x] Ajouter un CTRL+Z et CTRL+Y dans l'éditeur de code pour stocker les derniers états du code et pouvoir revenir en arrière ou aller en avant.
- [x] Pas de tooltip lorsqu'ils y'a une erreur dans un chammp.
- [x] Dans enchantment data driven, effects.minecraft:attributes.id l'identifiant agis comme un select hors c'est un champ de texte il faut vérifier la sémantiques dans notre code pour ce genre de cas.
- [x] Visuelle incorect y'a une seconde bordure rouge en plus de la premiére, et ont a rond rouge plein au lieu d'un rond rouge outline avec exclaamation. Il doit se trouver a droite et pas a gauche.