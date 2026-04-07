---
name: compose-reactivity
description: >
  Expert knowledge base on Compose/Kotlin reactivity: state management, recomposition,
  side effects, stability, and performance. Use this skill whenever the user asks about
  Jetpack Compose state, recomposition, mutableStateOf, remember, derivedStateOf,
  snapshotFlow, LaunchedEffect, collectAsStateWithLifecycle, stability annotations,
  @Stable/@Immutable, UDF, state hoisting, or any Compose reactivity patterns,
  anti-patterns, or architecture. Also trigger for questions like "how to avoid
  recomposition", "my Compose is slow", "state not updating", or "best way to handle
  state in Compose".
---

# Compose Reactivity — Expert Knowledge Base
> Sources: Google Android Developers docs (updated 2026-01-16), Leland Richardson (Anthropic/ex-Google Compose creator), Zach Klippenstein (Google Compose team), Manuel Vivo (Google), community experts 2025–2026.

## Table of Contents
1. [Mental Model fondamental](#1-mental-model-fondamental)
2. [Les 3 phases de Compose](#2-les-3-phases-de-compose)
3. [State — APIs et quand les utiliser](#3-state--apis-et-quand-les-utiliser)
4. [Stabilité — le concept le plus sous-estimé](#4-stabilité--le-concept-le-plus-sous-estimé)
5. [Side Effects — le guide complet](#5-side-effects--le-guide-complet)
6. [Collecte de Flow en Compose](#6-collecte-de-flow-en-compose)
7. [Patterns — les meilleurs choix](#7-patterns--les-meilleurs-choix)
8. [Anti-Patterns — tout ce qu'il faut éviter](#8-anti-patterns--tout-ce-quil-faut-éviter)
9. [Performance & Debugging](#9-performance--debugging)
10. [Architecture recommandée 2025-2026](#10-architecture-recommandée-2025-2026)

---

## 1. Mental Model fondamental

> *"Compose UI is a function of state. When state changes, affected composables are re-invoked."* — Google Android Docs

**Principe cardinal (UDF — Unidirectional Data Flow):**
- **State flows DOWN** (données descendent dans l'arbre)
- **Events flow UP** (événements remontent vers la source de vérité)

Toute violation de ce principe génère des bugs et des recompositions inattendues.

---

## 2. Les 3 phases de Compose

Compose transforme l'état en pixels via 3 phases **séquentielles** :

```
Composition → Layout → Drawing
```

| Phase | Ce qui se passe | Lecture d'état ici → |
|-------|----------------|----------------------|
| **Composition** | Exécute les fonctions composables, construit l'arbre UI | Déclenche recomposition si état change |
| **Layout** | Mesure et positionne les éléments | Déclenche re-layout uniquement |
| **Drawing** | Rend les pixels sur l'écran | Déclenche re-draw uniquement |

**Optimisation clé** : lire l'état le plus tard possible dans les phases pour minimiser la portée de la recomposition. Si l'état n'est lu qu'en Drawing, seule cette phase est rejouée.

```kotlin
// ❌ MAL — lecture en Composition → déclenche recomposition entière
val color by animateColorBetween(Color.Cyan, Color.Magenta)
Box(Modifier.fillMaxSize().background(color))

// ✅ BIEN — lecture en Drawing → skip Composition + Layout
val color by animateColorBetween(Color.Cyan, Color.Magenta)
Box(Modifier.fillMaxSize().drawBehind { drawRect(color) })
```

---

## 3. State — APIs et quand les utiliser

### 3.1 `mutableStateOf` + `remember` — La base

```kotlin
// Forme canonique recommandée (by délégation)
var count by remember { mutableStateOf(0) }

// Forme explicite (nécessaire si on passe State<T> à une autre fonction)
val countState: MutableState<Int> = remember { mutableStateOf(0) }
```

- `mutableStateOf` : rend la valeur **observable** → déclenche recomposition quand change
- `remember` : **préserve** la valeur entre recompositions (sinon reset à chaque recompo)
- Sans `remember` → nouvelle instance créée à chaque recomposition → pas d'observation utile

**Policies de comparaison :**
```kotlin
// structuralEqualityPolicy (défaut) — compare par equals()
mutableStateOf("hello") // compare par valeur

// referentialEqualityPolicy — compare par référence (===)
mutableStateOf(myObject, referentialEqualityPolicy())

// neverEqualPolicy — recompose toujours
mutableStateOf(myObject, neverEqualPolicy())
```

### 3.2 `rememberSaveable` — Survit aux rotations

```kotlin
var name by rememberSaveable { mutableStateOf("") }
// Survit : rotation, mise en arrière-plan, retour
// Ne survit pas : navigation (sauf avec SavedStateHandle dans ViewModel)
```

### 3.3 `derivedStateOf` — État dérivé optimisé

**Règle d'or** : utiliser quand l'entrée change souvent mais la sortie rarement.

```kotlin
// ❌ MAL — recompose à chaque scroll pixel
val showButton = listState.firstVisibleItemIndex > 0

// ✅ BIEN — recompose seulement quand le boolean change
val showButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

**Anti-pattern fréquent** : utiliser `derivedStateOf` pour des calculs simples qui devraient changer aussi souvent que leurs entrées (`"$firstName $lastName"` → **ne pas** utiliser derivedStateOf).

**Piège** : créer le `derivedStateOf` hors d'un `remember` → recréé à chaque recomposition → aucun bénéfice.

### 3.4 `snapshotFlow` — Pont State → Flow (coroutines)

```kotlin
LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex }
        .map { it > 5 }
        .distinctUntilChanged()
        .debounce(300)
        .collect { show -> viewModel.onScrollThreshold(show) }
}
```

**Différence fondamentale avec `derivedStateOf`** :

| | `derivedStateOf` | `snapshotFlow` |
|--|--|--|
| Retourne | `State<T>` (pour l'UI) | `Flow<T>` (pour coroutines/ViewModel) |
| Usage | Optimiser recompositions | Side effects / analytics / ViewModel |
| Déclenche recomposition | Oui (si lu en composition) | Non |
| Opérateurs Flow | Non | Oui (debounce, filter, etc.) |

**Anti-pattern critique** :
```kotlin
// ❌ CATASTROPHIQUE — recompose chaque frame d'animation
val progress by snapshotFlow { animationState.progress }
    .collectAsState(initial = 0f)

// ✅ CORRECT — pour analytics sans recomposition
LaunchedEffect(Unit) {
    snapshotFlow { animationState.progress }
        .distinctUntilChanged { old, new -> (old * 100).toInt() == (new * 100).toInt() }
        .collect { analytics.log(it) }
}
```

### 3.5 `rememberUpdatedState` — Lambda toujours fraîche

```kotlin
@Composable
fun AutoSave(content: String, onSave: (String) -> Unit) {
    val currentOnSave by rememberUpdatedState(onSave) // Capture la dernière version
    
    LaunchedEffect(Unit) { // Ne redémarre pas quand onSave change
        while (true) {
            delay(30_000)
            currentOnSave(content) // Appelle toujours la lambda la plus récente
        }
    }
}
```

**Quand utiliser** : quand un `LaunchedEffect` avec clé constante doit utiliser des valeurs qui changent sans redémarrer l'effet.

### 3.6 `produceState` — Non-Compose → Compose State

```kotlin
val uiState by produceState<UiState>(initialValue = UiState.Loading) {
    val data = withContext(Dispatchers.IO) { repository.fetch() }
    value = UiState.Success(data)
}
```

Utilisé pour convertir des sources non-Compose (callbacks, Flow, etc.) en `State<T>`.

---

## 4. Stabilité — le concept le plus sous-estimé

### 4.1 Pourquoi c'est critique

Si les paramètres d'un composable sont **instables**, Compose **recompose toujours** même si les valeurs n'ont pas changé. C'est la source #1 de surperformance invisible.

```
Stable parameters + unchanged → Compose SKIPS recomposition ✅
Unstable parameters → Compose ALWAYS recomposes ❌
```

### 4.2 Règles de stabilité du compilateur

**Stable par défaut :**
- Primitives : `Int`, `Long`, `Float`, `Double`, `Boolean`, `Char`, `String`
- Types fonctions (lambdas)
- `data class` avec uniquement des `val` de types stables, dans le **même module**

**Instable par défaut :**
- `var` dans une data class → **toute la classe devient instable**
- `List<T>`, `Map<K,V>`, `Set<T>` → instables car interfaces mutables possibles
- Classes d'un **autre module** (y compris multi-module interne) → instables par défaut
- Classes tierces (ex: `java.time.Instant`) → instables

```kotlin
// ❌ INSTABLE — var + List
data class UiState(
    var name: String,           // var → INSTABLE
    val items: List<String>     // List → INSTABLE
)

// ✅ STABLE
@Immutable
data class UiState(
    val name: String,
    val items: ImmutableList<String> // kotlinx.collections.immutable
)
```

### 4.3 `@Immutable` vs `@Stable`

| Annotation | Promesse | Utilisation |
|--|--|--|
| `@Immutable` | Aucune propriété ne change jamais après construction | DTO, états de lecture pure |
| `@Stable` | Les propriétés peuvent changer **mais** le runtime Compose est notifié | Classes avec MutableState |

**⚠️ DANGER** : annoter `@Immutable` une classe dont les propriétés changent → Compose skip des recompositions légitimes → bugs UI silencieux.

### 4.4 Problème multi-module

```kotlin
// Module :data — classe vue comme instable par :feature
data class User(val id: String, val name: String)

// Solutions :
// Option 1 — Ajouter compose-runtime-annotation au module :data
@Immutable
data class User(val id: String, val name: String)

// Option 2 — UI wrapper model dans :feature
@Immutable
data class UserUiModel(val id: String, val name: String)

// Option 3 — stability config file
// compose_compiler_config.conf :
// com.example.data.User=Stable
```

### 4.5 Diagnostiquer la stabilité

Activer les rapports du compilateur :
```groovy
// build.gradle
kotlinOptions {
    freeCompilerArgs += [
        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.buildDir}/compose-metrics",
        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.buildDir}/compose-reports"
    ]
}
```

Résultat type dans le rapport :
```
restartable skippable fun MyComposable(   // ✅ optimal
    stable name: String
    stable count: Int
)

restartable fun MyComposable(             // ❌ jamais skippable
    unstable items: List<Item>
)
```

---

## 5. Side Effects — le guide complet

> *"Composables should ideally be side-effect free."* — Google Android Docs
> *"LaunchedEffect(true) est aussi suspect que while(true)."* — Google Android Docs

### 5.1 Tableau de décision

| Besoin | API | Coroutine | Cleanup |
|--------|-----|-----------|---------|
| Lancer une coroutine lors d'un changement de clé | `LaunchedEffect` | ✅ | Auto (annulation) |
| Registrer/déregistrer un listener | `DisposableEffect` | ❌ | Manuel `onDispose {}` |
| Notifier un SDK non-Compose après recomposition | `SideEffect` | ❌ | Non |
| Convertir une source non-Compose en State | `produceState` | ✅ | Auto |
| Observer un Compose State en Flow | `snapshotFlow` | via LaunchedEffect | Auto |
| Lancer une coroutine sur action utilisateur | `rememberCoroutineScope` | ✅ | Non (scope) |

### 5.2 `LaunchedEffect` — Règles

```kotlin
// ✅ CORRECT — clé = dépendance réelle
LaunchedEffect(userId) {
    val data = fetchUser(userId) // Redémarre si userId change
    viewModel.setUserData(data)
}

// ⚠️ VALIDE mais à justifier explicitement
LaunchedEffect(Unit) {
    // Tourne une fois, lié au cycle de vie du composable
}

// ❌ ANTI-PATTERN — même chose que Unit, moins clair
LaunchedEffect(true) { ... }
```

**Règle des clés** : toute variable mutable **utilisée dans le bloc** doit soit être une clé, soit être wrappée dans `rememberUpdatedState`.

### 5.3 `DisposableEffect` — Règles

```kotlin
// ✅ CORRECT
DisposableEffect(lifecycleOwner) { // lifecycleOwner comme clé = redémarre si change
    val observer = LifecycleEventObserver { _, event ->
        val currentOnStart by rememberUpdatedState(onStart) // Lambda fraîche
        if (event == Lifecycle.Event.ON_START) currentOnStart()
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer) // ⚠️ OBLIGATOIRE
    }
}
```

**Anti-patterns critiques** :
- Oublier `onDispose {}` → fuite mémoire garantie
- Oublier `lifecycleOwner` comme clé → observe le mauvais lifecycle après navigation

### 5.4 `SideEffect` — Usage limité

```kotlin
// ✅ Cas d'usage : synchroniser un SDK après chaque recomposition
SideEffect {
    analytics.setUserProperty("theme", currentTheme) // Idempotent, rapide
}

// ❌ JAMAIS : opérations async, réseau, I/O
SideEffect {
    networkCall() // FAUX — s'exécute après chaque recomposition
}
```

---

## 6. Collecte de Flow en Compose

### 6.1 `collectAsStateWithLifecycle` — **LA RECOMMANDATION GOOGLE 2025**

```kotlin
// Dépendance requise
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

// ✅ RECOMMANDÉ par Google pour apps Android
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

**Avantages vs `collectAsState`** :
- Arrête la collecte quand le composable n'est plus visible (lifecycle < STARTED)
- Utilise `repeatOnLifecycle` sous le capot
- Économise CPU, batterie, mémoire
- Évite les recompositions inutiles en arrière-plan

**⚠️ Piège connu** : si un flow de navigation/auth n'est pas collecté en arrière-plan, l'état peut être périmé au retour → utiliser `stateIn(WhileSubscribed(5_000))` dans le ViewModel pour buffer.

### 6.2 `collectAsState` — Quand l'utiliser

```kotlin
// ✅ Pour Compose Multiplatform (iOS, Desktop) — pas de lifecycle Android
val state by myFlow.collectAsState(initial = defaultValue)
```

### 6.3 Pattern ViewModel recommandé

```kotlin
// ViewModel
val uiState: StateFlow<UiState> = repository.dataFlow
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000), // 5s de grace au back
        initialValue = UiState.Loading
    )

// Composable
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

---

## 7. Patterns — les meilleurs choix

### 7.1 State Hoisting — Règle des 3

1. Hoist au moins au niveau du **plus bas ancêtre commun** qui lit l'état
2. Hoist au moins au niveau du **plus haut endroit où l'état est modifié**
3. Si deux états changent en réponse aux **mêmes événements** → hoist ensemble

```kotlin
// ✅ PATTERN : stateful + stateless version
@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    SearchScreenContent(
        query = query,
        onQueryChange = viewModel::onQueryChange
    )
}

@Composable // Stateless = testable, réutilisable, previewable
fun SearchScreenContent(
    query: String,
    onQueryChange: (String) -> Unit
) { ... }
```

### 7.2 Déférer les lectures d'état (State Read Deferral)

```kotlin
// ❌ MAL — lecture au niveau Parent → Parent recompose quand scrollOffset change
@Composable
fun Parent() {
    val scrollState = rememberScrollState()
    Header(offset = scrollState.value.dp) // Lecture ici = Parent recompose
}

// ✅ BIEN — passer une lambda, lecture différée dans l'enfant
@Composable
fun Parent() {
    val scrollState = rememberScrollState()
    Header(offsetProvider = { scrollState.value.dp }) // Lambda = pas de lecture ici
}

@Composable
fun Header(offsetProvider: () -> Dp) {
    val offset = offsetProvider() // Lecture ici = seul Header recompose
    ...
}
```

### 7.3 Modifiers lambda pour animations

```kotlin
// ❌ MAL — lit animatedValue en composition → recompose à chaque frame
Text(modifier = Modifier.offset(y = 60.dp * animatedValue))

// ✅ BIEN — lit en Layout phase → zéro recomposition
Text(modifier = Modifier.offset { IntOffset(0, (60.dp * animatedValue).roundToPx()) })
```

### 7.4 `@Stable` pour les State Holders

```kotlin
@Stable
class MyScreenState(
    private val windowSizeClass: WindowSizeClass
) {
    var isExpanded by mutableStateOf(false)
    val shouldShowNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
}

@Composable
fun rememberMyScreenState(windowSizeClass: WindowSizeClass): MyScreenState {
    return remember(windowSizeClass) { MyScreenState(windowSizeClass) }
}
```

### 7.5 Granularité de l'état ViewModel

```kotlin
// ❌ MAL — un seul StateFlow massif → une modification recompose tout
data class UiState(
    val isLoading: Boolean,
    val user: User?,
    val items: List<Item>,
    val selectedItem: Item?,
    val error: String?
)

// ✅ MIEUX — flows séparés pour parties indépendantes
class MyViewModel : ViewModel() {
    val isLoading = MutableStateFlow(false)
    val user = MutableStateFlow<User?>(null)
    val items = MutableStateFlow<ImmutableList<Item>>(persistentListOf())
    // Chaque composable subscribe seulement à ce dont il a besoin
}
```

---

## 8. Anti-Patterns — tout ce qu'il faut éviter

### ❌ AP-1 : Backward Write (le plus grave)

```kotlin
@Composable
fun BadComposable() {
    var count by remember { mutableIntStateOf(0) }
    Text("$count")
    count++ // ⚠️ ÉCRITURE après LECTURE → recomposition infinie
}
```
**Règle Google** : ne jamais écrire de l'état qui a déjà été lu dans la même composition. Écrire uniquement dans des event handlers (onClick, etc.).

### ❌ AP-2 : Composition Loop via onSizeChanged

```kotlin
// ❌ MAL — modifie l'état en layout → déclenche composition → déclenche layout → ...
Box {
    var heightPx by remember { mutableIntStateOf(0) }
    Image(modifier = Modifier.onSizeChanged { heightPx = it.height })
    Text(modifier = Modifier.padding(top = heightPx.dp)) // Loop !
}

// ✅ BIEN — utiliser les primitives de layout
Column { Image(); Text() }
```

### ❌ AP-3 : mutableStateOf sans remember

```kotlin
@Composable
fun Broken() {
    var count by mutableStateOf(0) // ❌ Sans remember = reset à chaque recompo
    Button(onClick = { count++ }) { Text("$count") }
}
```

### ❌ AP-4 : derivedStateOf hors de remember

```kotlin
// ❌ Recréé à chaque recomposition = aucun bénéfice
val showButton = derivedStateOf { listState.firstVisibleItemIndex > 0 }

// ✅
val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
```

### ❌ AP-5 : collectAsState au niveau Screen (state trop global)

```kotlin
// ❌ MAL — chaque changement dans UiState recompose tout le Screen
@Composable
fun Screen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    Column {
        Header(uiState) // recompose même si seul le footer change
        Body(uiState)
        Footer(uiState)
    }
}

// ✅ MIEUX — chaque sous-composant subscribe à ce dont il a besoin
@Composable
fun Screen(viewModel: MyViewModel) {
    Column {
        Header(viewModel) // subscribe au flux header seulement
        Body(viewModel)   // subscribe au flux body seulement
        Footer(viewModel) // subscribe au flux footer seulement
    }
}
```

### ❌ AP-6 : Passer MutableState comme paramètre

```kotlin
// ❌ ANTI-PATTERN — joint ownership, brise UDF
@Composable
fun MyInput(state: MutableState<String>) {
    TextField(value = state.value, onValueChange = { state.value = it })
}

// ✅ CORRECT — stateless avec callback
@Composable
fun MyInput(value: String, onValueChange: (String) -> Unit) {
    TextField(value = value, onValueChange = onValueChange)
}
```

### ❌ AP-7 : Passer ViewModel dans les sous-composables

```kotlin
// ❌ MAL — couplage fort, impossible à tester/previewer
@Composable
fun UserCard(viewModel: UserViewModel) { ... }

// ✅ BIEN — passer les données et lambdas
@Composable
fun UserCard(user: User, onAction: (UserAction) -> Unit) { ... }
```

### ❌ AP-8 : LaunchedEffect avec clé incorrecte

```kotlin
// ❌ Trop peu de clés — n'observe pas userId changes
LaunchedEffect(Unit) {
    viewModel.loadUser(userId) // userId n'est pas une clé !
}

// ❌ Trop de clés — redémarre inutilement
LaunchedEffect(userId, isLoading, error) {
    viewModel.loadUser(userId) // isLoading et error ne changent pas le comportement
}

// ✅ Exactement les bonnes clés
LaunchedEffect(userId) {
    viewModel.loadUser(userId)
}
```

### ❌ AP-9 : Lambda capturant l'état inline

```kotlin
// ❌ MAL — nouvelle lambda à chaque recomposition → instabilité
@Composable
fun Parent() {
    val count by remember { mutableStateOf(0) }
    ChildComposable(onClick = { doSomething(count) }) // lambda recrée = recompose Child
}

// ✅ BIEN
@Composable
fun Parent() {
    val count by remember { mutableStateOf(0) }
    val onClick = remember(count) { { doSomething(count) } }
    ChildComposable(onClick = onClick)
}
```

### ❌ AP-10 : CompositionLocal pour état non-global

```kotlin
// ❌ MAL — dépendance implicite, dur à tester, à débugger
val LocalCurrentUser = compositionLocalOf<User> { error("No user") }

// ✅ BIEN — passer l'utilisateur explicitement comme paramètre
@Composable
fun UserProfile(user: User) { ... }
```

### ❌ AP-11 : snapshotFlow + collectAsState pour l'UI

```kotlin
// ❌ CATASTROPHIQUE — recompose chaque frame si state change souvent
val scrollPos by snapshotFlow { listState.firstVisibleItemIndex }
    .collectAsState(initial = 0)

// ✅ snapshotFlow → pour side effects seulement, pas pour l'UI
// Pour l'UI → derivedStateOf
```

### ❌ AP-12 : Opérations lourdes en composition

```kotlin
// ❌ MAL — calcul lourd à chaque recomposition
@Composable
fun ContactList(contacts: List<Contact>) {
    val sorted = contacts.sortedBy { it.name } // Recalculé à chaque recompo
    ...
}

// ✅ BIEN
@Composable
fun ContactList(contacts: List<Contact>) {
    val sorted = remember(contacts) { contacts.sortedBy { it.name } }
    ...
}
```

### ❌ AP-13 : collectAsState sans lifecycle-awareness (Android)

```kotlin
// ❌ Collecte en arrière-plan, gaspille batterie/CPU
val state by viewModel.state.collectAsState()

// ✅ S'arrête quand pas visible
val state by viewModel.state.collectAsStateWithLifecycle()
```

---

## 9. Performance & Debugging

### 9.1 Outils de diagnostic

1. **Android Studio Layout Inspector** → onglet "Recomposition" → affiche compteurs
2. **Compose Metrics** → rapports de stabilité via compiler flags
3. **Tracing systrace** → "Composition" spans dans Perfetto

### 9.2 Compiler metrics

```groovy
android { kotlinOptions { freeCompilerArgs += [
    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=build/compose-metrics",
    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=build/compose-reports"
]}}
```

### 9.3 Checklist de performance

- [ ] Tous les paramètres de composables critiques sont stables ?
- [ ] `List<T>` remplacées par `ImmutableList<T>` ou annotées `@Immutable` ?
- [ ] Animations via lambda modifiers (`offset { }`, `drawBehind { }`) ?
- [ ] `derivedStateOf` pour états dérivés depuis états fréquemment changeants ?
- [ ] Lambdas callback wrappées dans `remember` ou issues de ViewModel stable ?
- [ ] `key { }` dans `LazyColumn`/`LazyRow` pour tous les items ?
- [ ] Lectures d'état aussi proches que possible du leaf composable ?
- [ ] `collectAsStateWithLifecycle` partout (non multiplatform) ?

---

## 10. Architecture recommandée 2025-2026

```
ViewModel (StateFlow<UiState>)
    ↓ WhileSubscribed(5_000)
Screen Composable
    ↓ collectAsStateWithLifecycle()
    ├── Header (stable params)
    ├── Body  (stable params)
    └── Footer (stable params)
         ↑ lambda events (User → ViewModel)
```

**État UI** : `@Immutable data class` avec `ImmutableList` pour les collections
**Persistance locale** : `rememberSaveable` pour champs UI non-triviaux
**Global app state** : `StateHolder` pattern (pas de Singleton, pas de God ViewModel)
**Navigation events** : `Channel<T>` côté ViewModel, `LaunchedEffect(viewModel)` côté UI

---

## Références experts

| Expert | Affiliation | Contributions clés |
|--------|-------------|-------------------|
| Leland Richardson | Anthropic / ex-Google Compose creator | Fondations compilateur Compose, remember |
| Zach Klippenstein | Google Compose team | Snapshot system, scoped recomposition |
| Manuel Vivo | Google | collectAsStateWithLifecycle, UDF |
| Adam Powell | Google | Runtime Compose, recomposer internals |
| Jorge Castillo | Community | Effect handlers |
| skydoves (Jaewoong Eum) | Community/Stream | Stability inference, tooling |

## Ressources canoniques

- https://developer.android.com/develop/ui/compose/state (mis à jour 2026)
- https://developer.android.com/develop/ui/compose/performance (mis à jour 2026)
- https://developer.android.com/develop/ui/compose/side-effects
- https://developer.android.com/develop/ui/compose/performance/stability
- https://developer.android.com/develop/ui/compose/phases
- https://manuelvivo.dev/consuming-flows-compose