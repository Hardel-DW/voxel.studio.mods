# Les tickets organisés par catégories 

## Les Bugs d'interface
- [] L"icone dans la créations de pack n'est pas utilisée.
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

# Refactor
4. Il y a beaucoup de catch (Exception ignored) et de flux silencieux, ce qui masque les bugs de ressources/UI et rend le diagnostic pénible. Voir VoxelStudioWindow.java#L198, SvgIcon.java#L30, ResourceImageIcon.java#L34, BrowserUtils.java#L5.
--------



Analyse du 19 mars - C'était des bugs JavaFX il sont peut être d'actualités vu qu'ont est sur Compose. A verifier. 

Autre Bugs:
100. Un reload de ressources reconstruit tout StudioEditorRoot sans réinjecter immédiatement permissions, packs, tabs ou route courante. Le résultat probable est un retour transitoire ou durable sur un contexte incohérent après reload. Voir VoxelStudioWindow.java#L157, VoxelStudioWindow.java#L136, VoxelStudioWindow.java#L188.

103. Côté perf/rendering, plusieurs vues reconstruisent et re-trient tout sur chaque changement: overview, tree, rebuild du tree de concept, reset du scroll. Ça tient sur un registre petit, mais ça ne passera pas proprement à recipes/loot tables/structures. Voir EnchantmentOverviewPage.java#L135, InfiniteScrollPane.java#L33, EnchantmentLayout.java#L50, FileTreeView.java#L49.

104. La génération de l’atlas items est coûteuse et lancée sur le render thread pour tous les items d’un coup. En plus, un ItemSprite construit avant la disponibilité de l’atlas reste caché jusqu’à un refresh ultérieur sans mécanisme de resubscribe. Voir ItemAtlasRenderer.java#L96, GameRendererMixin.java#L13, ItemSprite.java#L18.
105. 
1.
Mais il y a un subtilité avec les lower layers : quand PackA est modifié, les workspaces de PackB/C qui ont été chargés après PackA ne sont pas recalculés. Si PackA modifie un tag et PackB dépend de ce tag,   
PackB ne verra pas le changement tant que son workspace n'est pas rechargé.

2.
Si un WorkspaceSyncPayload arrive quand la fenêtre est fermée (activeGateway == null), la mise à jour est perdue silencieusement. Si l'utilisateur réouvre la fenêtre, il aura un état stale jusqu'au prochain   
requestPackWorkspace.

3.
Si le serveur ne répond jamais (crash réseau, timeout), les pending actions restent indéfiniment dans la map. Pas de nettoyage périodique, seulement au resetForWorldSync/resetForWorldClose.

4.
Chaque mutation fait un flushDirty() qui écrit sur disque de manière synchrone sur le server thread. Si le disque est lent, ça bloque le thread serveur.