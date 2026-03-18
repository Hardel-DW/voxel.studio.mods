# Les tickets organisés par catégories 

## Les Bugs d'interface
- [] À traiter en dernier car mineur, quand on arrive sur une page pendant une frame leur taille semble différente ce qui crée un scroll temporaire de quelques millisecondes, sûrement un rendu initial différent.
- [] Certains cases dans "Items supportés" ne fonctionne pas, peut être que le tags n'existe pas ou autre chose. Inclus épée, bouclier, je crois que c'est les tags voxel.
- [] Dans Exclusive Set, parfois l'action ne fait rien dans un cas qui semble éxtrémement précis. ça semble être :
    * Arriver sur la page avec une cible déjà existante e.g "Armor"
    * Désactiver "Armor" fonctionne visuellement mais ne fait rien au flush, (Si je change de page ça fonctionne)
    * Les autres tags fonctionnent correctement.
    * Si j'arrive et que l'enchantment n'a aucunes cibles, ça marche correctement pour tout le monde.

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


--------


5. Un reload de ressources reconstruit tout StudioEditorRoot sans réinjecter immédiatement permissions, packs, tabs ou route courante. Le résultat probable est un retour transitoire ou durable sur un contexte incohérent après reload. Voir VoxelStudioWindow.java#L157, VoxelStudioWindow.java#L136, VoxelStudioWindow.java#L188.

7. Les actions réseau ne sont pas validées défensivement côté serveur. Un payload malformé peut faire lever l’interpréteur (EquipmentSlotGroup.valueOf, etc.) sur le thread serveur. Voir AssetEditorNetworking.java#L90 et EnchantmentInterpreter.java#L36.

8. L’abstraction “générique” est en réalité durcie sur enchantment, et en plus elle s’appuie souvent sur registryId.getPath() comme clé globale. C’est fragile, collision-prone, et mauvais pour l’extension future à d’autres registres ou namespaces. Voir ActionInterpreter.java#L10, AssetEditorNetworking.java#L32, ServerElementStore.java#L217, RegistryElementStore.java#L136.

10. Côté perf/rendering, plusieurs vues reconstruisent et re-trient tout sur chaque changement: overview, tree, rebuild du tree de concept, reset du scroll. Ça tient sur un registre petit, mais ça ne passera pas proprement à recipes/loot tables/structures. Voir EnchantmentOverviewPage.java#L135, InfiniteScrollPane.java#L33, EnchantmentLayout.java#L50, FileTreeView.java#L49.

11. La génération de l’atlas items est coûteuse et lancée sur le render thread pour tous les items d’un coup. En plus, un ItemSprite construit avant la disponibilité de l’atlas reste caché jusqu’à un refresh ultérieur sans mécanisme de resubscribe. Voir ItemAtlasRenderer.java#L96, GameRendererMixin.java#L13, ItemSprite.java#L18.

13. Il y a beaucoup de catch (Exception ignored) et de flux silencieux, ce qui masque les bugs de ressources/UI et rend le diagnostic pénible. Voir VoxelStudioWindow.java#L198, SvgIcon.java#L30, ResourceImageIcon.java#L34, BrowserUtils.java#L5.

14. Le flux de création de pack est peu propre: l’UI a un champ icône non utilisé, le serveur ne renvoie pas d’erreur explicite, et le client ferme le dialog même si la création échoue. Voir PackCreateDialog.java#L23, StudioPackState.java#L69, AssetEditorNetworking.java#L155.