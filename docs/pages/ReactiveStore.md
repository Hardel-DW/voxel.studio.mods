# Reactive Store / Selector (Zustand Like)
Le système de selectors doit être une couche technique indépendante de l’application, Son rôle n’est pas de connaître Minecraft, les packs, les enchantments, les tags, Compose ou l’UI.
la lib selector fournit le moteur, l'application les données et les selecteurs. L'ui/pages consomme les selecteurs.

Son seul rôle est stocker un état permettre de sélectionner une partie de cet état, notifier seulement si la valeur sélectionnée a réellement changé

La logique métier n’écrit pas dans ce module, L’application l’utilise, mais ne le mélange pas avec ses propres règles.

### Lib Selector Core
Cette lib doit être générique et réutilisable, potentiellement extractible plus tard dans un autre repository, Elle contient uniquement :
- un store générique
- un système de selector
- un système de subscription / unsubscribe
- une comparaison d’égalité pour éviter les updates inutiles

Elle ne contientera pas de logiques comme :
- ElementEntry
- Registry
- Identifier
- Pack
- EditorAction
- Gateway
- Compose
- I18n

### Application
L’application garde ses propres états et écrit ses propres selectors métier Exemples, Les selectors métier appartiennent à l’application, pas à la lib. Exemple.
- selectIsTagEnabled(tagId)
- selectCurrentElement()

/!\
La gateway modifie l’état, mais ne fait pas partie de la lib selector