# Les tickets organisés par catégories 

## Les Bugs
- Dans la page Non Combinable, le bouton action ne fait rien.
- Les 4 costs dans les paramètres techniques semblent pas bind. Ils sont à 0 chacun.
- Dans la page "Items supportés", il y a un énorme espace vide en dessous de la page, je peux scroll dessous alors qu'il n'y a pas de contenu.
- Fait un tour de toute l'app pour chercher les strings pas traduites.
- À traiter en dernier car mineur, quand on arrive sur une page pendant une frame leur taille semble différente ce qui crée un scroll temporaire de quelques millisecondes, sûrement un rendu initial différent.
- (CRITIQUE) Lorsque je met un enchantment dans un tags exemple "curse" il définit bien le fichier tags curse.json, mais quand je le retire il n'est pas supprimé du fichier tags curse.json. Cela ce produit seulement si c'est le dernier élément du tags. Par exemple si j'ai "sharpness" et "breach" dans curse, quand je retire "breach" il seras correctement retiré, du coup il reste que "sharpness" dans curse.json. Si je tente de retirer "sharpness" il n'est pas retiré du fichier tags curse.json. Je pense que le fichier tags n'est pas correctement supprimé quand y'a plus aucun élément dans le tags.
- (CRITIQUE) J'ai défini curse lors d'une session avec "sharpenss" dedans, j'ai relancer le monde/jeu réouvert l'interface, jusqu'a la valeur est bonne correctement afficher dans l'interface et le json mais si je met un autre enchantment dedans, il supprime tous ce qui s'y trouvait dedans. Donc sharpness est supprimé du tags.

## Le UI/UX (Interface utilisateur)
- Dans la sidebar enchantment "Exclusif" les noms des éléments sont mal affichés, voici un exemple de ce qui est affiché : "exclusive_set/bow" alors qu'on voudrait "Bow".
- Dans le même registre on voit un élément nommé "none" qui n'est pas censé exister.
- Dans overview, afficher par ordre alphabétique les éléments par leurs noms.
- Certains bugs mineurs (aussi présents dans la version web) je ne peux pas collapse les Node de l'arbre quand un élément est sélectionné. (Ce bug concerne l'onglet items et exclusive, mais pas slots)
- Dans Header le gradient de couleur n'est pas exactement le même que dans la version web, il semble plus brutal, il faut que le gradient soit plus doux.
- La couleur dans la sidebar doit être la même que la couleur du header.
- Dans les tabs on voit "bane_of_arthropods" alors qu'on voudrait "Bane of Arthropods" basé sur le même système de traduction que Header.
- La page overview de enchantment faut vérifier les padding/couleurs/bordures, il semble différent. Le web semble plus foncé et mieux intégré.
- Les "slots" dans la sidebar et dans la page "slots" semblent pas traduits, je vois "Main Hand" alors que je suis en français.
- Idem les descriptions en dessous sont en anglais.
- Pour les Tags donc Items et Exclusif ça semble compliqué d'avoir une trad. Normalement il y a une convention fabric pour ça mais je ne sais pas si c'est pertinent de l'utiliser.
- La barre de recherche prend toute la longueur. La version web a une width max.
- Vérifie Height/Padding/Gap dans le Header. Il semble être différent de quelques pixels.
- Les bordures semblent plus claires que la version web.
- Dans la page Non Combinable, le bouton action doit être en bas à droite, à cause du "Grid" le bouton essaye de se mettre en bas, mais le parent ne prend pas toute la hauteur, donc il n'est pas totalement en bas.
- "See More" dans ce même composant Non Combinable n'est pas traduit. De plus le hover ne déclenche pas le Popover pour voir les éléments supplémentaires.
- Il faut personnaliser le scroll en se référant au design de la version web dans global.css.

## Les Performances