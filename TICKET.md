# Les tickets organisés par catégories 

## Les Bugs
- [] Dans la page Non Combinable, le bouton action ne fait rien. Il est censés ouvrir le popover avec les deux actions possibles. (Actuellement ont peut cliquer sur la carte ce qui n'a aucun sens)
- [] Dans la page "Items supportés", il y a un énorme espace vide en dessous de la page, je peux scroll dessous alors qu'il n'y a pas de contenu.
- [] Dans la page "Items supportés", il manque l'image de croix rouge. Il faut la copier de la version web.
- [] À traiter en dernier car mineur, quand on arrive sur une page pendant une frame leur taille semble différente ce qui crée un scroll temporaire de quelques millisecondes, sûrement un rendu initial différent.

## Le UI/UX (Interface utilisateur)
- Dans le Header et Breadcrumbs, le titre est parfois pas traduis. Exemple je suis sur la page "Main Principale" je vois "Mainhand" alors que je suis en français.
- [] La couleur dans la sidebar doit être la même que la couleur du header.
- [] Dans les breadcrumbs on voit "bane_of_arthropods" alors qu'on voudrait "Bane of Arthropods" basé sur le même système de traduction que Header.
- [] La page overview de enchantment faut vérifier les padding/couleurs/bordures, il semble différent. Le web semble plus foncé et mieux intégré.
- [] Pour les Tags donc Items et Exclusif ça semble compliqué d'avoir une trad. Normalement il y a une convention fabric pour ça mais je ne sais pas si c'est pertinent de l'utiliser.
- [] Vérifie Height/Padding/Gap dans le Header. Il semble être différent de quelques pixels.
- [] Les bordures semblent plus claires que la version web.
- [] Dans la page Non Combinable, le bouton action doit être en bas à droite, à cause du "Grid" le bouton essaye de se mettre en bas, mais le parent ne prend pas toute la hauteur, donc il n'est pas totalement en bas.

## Les Performances