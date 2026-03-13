# Les tickets organisés par catégories 

## Les Bugs
- [] À traiter en dernier car mineur, quand on arrive sur une page pendant une frame leur taille semble différente ce qui crée un scroll temporaire de quelques millisecondes, sûrement un rendu initial différent.
- [] Certains cases dans "Items supportés" ne fonctionne pas, peut être que le tags n'existe pas ou autre chose. Inclus épée, bouclier, je crois que c'est les tags voxel.
- [] Dans Exclusive Set, parfois l'action ne fait rien dans un cas qui semble éxtrémement précis. ça semble être :
    * Arriver sur la page avec une cible déjà existante e.g "Armor"
    * Désactiver "Armor" fonctionne visuellement mais ne fait rien au flush, (Si je change de page ça fonctionne)
    * Les autres tags fonctionnent correctement.
    * Si j'arrive et que l'enchantment n'a aucunes cibles, ça marche correctement pour tout le monde.

## Le UI/UX (Interface utilisateur)
- [] La couleur dans la sidebar doit être la même que la couleur du header.
- [] Les AnimatedTabs, donc les tabs avec l'inner qui bouge, au rendu initial la taille de cette inner n'est pas bonne, je pense qu'elle est calculée sur la version anglaises, ou je sais pas. (Aprés un clique elle se calibre correctement)
- [] Traduire tout les messages d'erreur avec i18n "error:*".
- [] Quand je clique sur un SVG (Pas tous) exemple celui dans le Tree, le Chevron qui permet d'expand, si je double clique ça réduit/augmente la fenétre. ça agis comme la title bar du window.
- [] Le bouton configurer ne fait rien dans overview. Il doit avoir la même action que quand ont clique sur la row.
- [] Dans la page "Items supportés", lorsque je réduit la fenétre et la remet en grand, la taille des cartes est abusément élévé en hauteur

## A refactor:
- [] Retravailler le I18n faire en sorte de ccentralisés absoluement toutes la logique entiére dans un même fichier et le rendre le plus générique possible, extensible gérer tout les cas au même endroit. Les Pages/Composant doivent juste appeler ces fonction et ne rien calculer. (Hormis une concaténation).

## Projet du futur pour la V1
- Travailler une API de debugging modernes et universelles pour le projet entier, qui soit propre, senior et future proof, générique et extensible.
- [] Travailler le debugging, pouvoir voir les éléménets re-render quel composant et la duréer d'un render par composant.
- [] Avoir des logs détaillés des actions, des erreurs/warnings et informations utiles de render long.
- [] Pouvoir voir le contenu de certaines variables dans le debugging.

- [] Créer la page "Vos Modification", qui permet de consulter les modifications effectuées dans le projet, et les annuler. Ont peut se calquer sur la version web.
    * [] La sidebar avec deux vues, une qui affiche par concept, une qui affiche par fichier.
    * [] Créer une API de highlight et de diff (Ont pourrais prendre l'api github et pour le JSON s'inspirer de l'api du package propriétaire que j'ai fait)
    * [] Un éditeur de texte rudimentaire pour le json, avec possibilité d'ouvrir VSC facilement.

- [] Créer le concept de Loot Table, qui permet de gérer les loot tables de Minecraft. Visuel calquer sur la version web.
- [] Créer le concept de Recipe. Visuel calquer sur la version web.