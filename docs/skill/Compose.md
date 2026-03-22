# Kotlin & Compose Multiplatform — Best Practices 2026
# CLAUDE.md — Skill de référence pour Claude Code

> Sources : Google Android Developers, JetBrains KMP Docs, Coil Docs, Ian Lake (Google),
> Ben Trengrove (Google), Don Turner (Google), Jorge Castillo (newsletter).
> Dernière mise à jour des sources vérifiées : 2025-2026.

---

## 1. RÉACTIVITÉ — ÉVITER LES RE-RENDER SUPERFLUS

### 1.1 Les 3 phases de Compose — règle fondamentale

> Source: developer.android.com/develop/ui/compose/phases
>
> "So, many of the performance best practices are to help Compose skip the phases it doesn't
> need to do. [...] Defer state reads for as long as possible. By moving state reading to a
> child composable or a later phase, you can minimize recomposition or skip the composition
> phase entirely."

**Règle : lire le state le plus tard possible dans l'arbre, et dans la phase la plus basse.**

Les 3 phases dans l'ordre : **Composition → Layout → Draw**
- Lire dans Draw = pas de recomposition du tout
- Lire dans Layout = pas de recomposition, juste relayout
- Lire dans Composition = recomposition complète du scope

### 1.2 Stabilité des types — source de recompositions involontaires

> Source: developer.android.com/develop/ui/compose/performance/stability
>
> "Compose considers collection classes unstable, such as List, Set and Map. This is because
> it cannot be guaranteed that they are immutable. You can use Kotlinx immutable collections
> instead or annotate your classes as @Immutable or @Stable."

**Règles :**

```kotlin
// ❌ List<> est instable → recomposition systématique
@Composable
fun UserList(users: List<User>) { ... }

// ✅ ImmutableList est stable → skip possible
@Composable
fun UserList(users: ImmutableList<User>) { ... }

// ✅ Ou wrapper @Immutable
@Immutable
data class UserList(val items: List<User>)
```

Dépendance requise :
```toml
# libs.versions.toml
kotlinx-collections-immutable = "0.3.8"
[libraries]
kotlinx-collections-immutable = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version.ref = "kotlinx-collections-immutable" }
```

> Source: developer.android.com/develop/ui/compose/performance/stability/fix
>
> "Warning: These annotations don't make a class immutable or stable on its own. Instead, by
> using these annotations you opting in to a contract with the compiler. Incorrectly annotating
> a class could cause recomposition to break. [...] You should be very careful about how you
> use these annotations."

**Ne pas abuser de `@Stable` / `@Immutable` sur des classes mutables — c'est un contrat.**

### 1.3 Strong Skipping Mode — activé par défaut depuis Kotlin 2.0.20

> Source: kotlinlang.org/docs/whatsnew2020.html
>
> "Strong skipping mode for the Compose compiler is now enabled by default. Strong skipping
> mode is a Compose compiler configuration option that changes the rules for what composables
> can be skipped. With strong skipping mode enabled, composables with unstable parameters can
> now also be skipped."

> Source: Ben Trengrove (Google), medium.com/androiddevelopers/jetpack-compose-strong-skipping-mode-explained
>
> "Strong skipping mode also enables more memoization of lambdas inside composable functions.
> [...] With strong skipping enabled, lambdas with unstable captures are also memoized. This
> means all lambdas written in composable functions are now automatically remembered."

**En 2026, avec Kotlin 2.0.20+, Strong Skipping est ON par défaut. Ce que ça change :**
- Les composables avec paramètres instables peuvent maintenant être skippés (via `===`)
- Toutes les lambdas dans les composables sont automatiquement mémoïsées

**Mais attention — caveat important :**

> Source: Jorge Castillo, newsletter.jorgecastillo.dev/p/strong-skipping-does-not-fix-kotlin-collections
>
> "With strong skipping mode, the runtime compares two instances via referential equality (===),
> instead of structural equality (equals()). [...] People tend to think that strong skipping
> fixes the issue with Kotlin collections [...] but this is not 100% true. Referential equality
> returns true if the List reference is the same; it does not care about its content."

→ Si tu crées une nouvelle `List` à chaque recomposition (ex: `listOf(...)` inline), Strong
Skipping ne suffit pas. Utiliser `ImmutableList` ou `remember`.

### 1.4 Différer la lecture de state — technique clé

> Source: developer.android.com/develop/ui/compose/performance/bestpractices
>
> "When the scroll state changes, Compose invalidates the nearest parent recomposition scope.
> [...] If you change your code to only read the state where you actually use it, then you
> could reduce the number of elements that need to be recomposed."

```kotlin
// ❌ Parent lit le scroll → tout le parent recompose à chaque scroll
@Composable
fun SnackDetail() {
    val scroll = rememberScrollState(0)
    Title(snack, scroll.value) // lit ici = scope du parent invalide
}

// ✅ Passer une lambda → la lecture est différée dans le child
@Composable
fun SnackDetail() {
    val scroll = rememberScrollState(0)
    Title(snack) { scroll.value } // la lecture a lieu dans Title
}
```

### 1.5 Lambda modifiers vs Dp modifiers

> Source: developer.android.com/develop/ui/compose/phases
>
> "Key point: This example is suboptimal because every scroll event results in the entire
> composable content being reevaluated [...] You can optimize the state read to only
> re-trigger the layout phase. There is another version of the offset modifier available:
> Modifier.offset(offset: Density.() -> IntOffset). This version takes a lambda parameter,
> where the resulting offset is returned by the lambda block."

```kotlin
// ❌ Dp modifier → lit dans la phase Composition → recompose à chaque scroll
Modifier.offset(
    with(LocalDensity.current) { (scroll.value / 2).toDp() }
)

// ✅ Lambda modifier → lit dans la phase Layout → pas de recomposition
Modifier.offset {
    IntOffset(x = 0, y = scroll.value / 2)
}

// ✅✅ graphicsLayer → lit dans la phase Draw → encore mieux pour animations
Modifier.graphicsLayer {
    translationY = scroll.value.toFloat()
}
```

> Source: Ben Trengrove (Google), medium.com/androiddevelopers/jetpack-compose-debugging-recomposition
>
> "Now that our Compose state is only being read inside the graphicsLayer modifier, we have
> deferred the read outside of composition and composition can be skipped. If we re-run the
> app and open the Layout Inspector we can see that composition has been skipped entirely."

**Règle : `Modifier.offset { }` > `Modifier.offset(dp)` pour les valeurs qui changent fréquemment.**
**Règle : `graphicsLayer { }` > tout le reste pour les animations frame-by-frame.**

### 1.6 derivedStateOf — filtrer les changements rapides

> Source: developer.android.com/develop/ui/compose/performance (checklist officielle)
>
> "Limit unnecessary recompositions: Use derivedStateOf to limit recompositions when rapidly
> changing state."

```kotlin
// ❌ Recompose à chaque pixel de scroll
val showButton by remember { scrollState.value > 100 } // pas un State

// ✅ Ne recompose que quand le booléen change (pas à chaque pixel)
val showButton by remember {
    derivedStateOf { scrollState.value > 100 }
}
```

**Règle : `derivedStateOf` quand le state source change souvent mais que l'UI n'a besoin
de réagir qu'à un sous-ensemble de changements.**

### 1.7 backward writes — crash garanti

> Source: developer.android.com/develop/ui/compose/performance/bestpractices
>
> "This code updates the count at the end of the composable after reading it on the preceding
> line. [...] you'll see that after you click the button, which causes a recomposition, the
> counter rapidly increases in an infinite loop."

```kotlin
// ❌ CRASH / boucle infinie — écriture dans la composition après lecture
@Composable
fun BadCounter() {
    var count by remember { mutableStateOf(0) }
    Text("Count: $count") // lecture
    count++ // écriture → recomposition → lecture → écriture → ...
}

// ✅ N'écrire que dans les event handlers (lambdas)
@Composable
fun GoodCounter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) { // écriture dans un event
        Text("Count: $count")
    }
}
```

### 1.8 remember pour les calculs coûteux

```kotlin
// ❌ Recalculé à chaque recomposition
val sortedItems = items.sortedBy { it.name }

// ✅ Mémoïsé, recalculé seulement quand items change
val sortedItems = remember(items) { items.sortedBy { it.name } }
```

### 1.9 LazyList — toujours passer des keys stables

> Source: developer.android.com/develop/ui/compose/performance (checklist officielle)
>
> "Help lazy layouts: Provide stable keys to lazy layouts using the key parameter to
> minimize unnecessary recompositions."

```kotlin
// ❌ Pas de key → Compose ne peut pas optimiser les moves
LazyColumn {
    items(users) { user -> UserRow(user) }
}

// ✅ Key stable → Compose réutilise les items existants
LazyColumn {
    items(users, key = { user -> user.id }) { user ->
        UserRow(user)
    }
}
```

---

## 2. ROUTING — NAVIGATION COMPOSE TYPE-SAFE (2.8+)

### 2.1 Règle fondamentale : no strings, type-safe routes

> Source: Ian Lake (Google), medium.com/androiddevelopers/navigation-compose-meet-type-safety
>
> "You should note one thing immediately: no strings! Specifically: No route string when
> defining a composable destination — specifying the type is enough to generate the route
> for you [...] No route string when navigating to a new destination."

```kotlin
// ❌ Ancien style — string-based, fragile, pas type-safe
navController.navigate("profile/42")
composable("profile/{id}") { ... }

// ✅ Nouveau style — type-safe, Navigation 2.8+
@Serializable
data class Profile(val id: String)

NavHost(navController, startDestination = Home) {
    composable<Home> {
        HomeScreen(onNavigate = { navController.navigate(Profile("42")) })
    }
    composable<Profile> { backStackEntry ->
        val profile: Profile = backStackEntry.toRoute()
        ProfileScreen(profile.id)
    }
}
```

### 2.2 Passer des arguments — ID uniquement, pas d'objets complets

> Source: developer.android.com/develop/ui/compose/navigation
>
> "Complex objects should be stored as data in a single source of truth, such as the data
> layer. Once you land on your destination after navigating, you can then load the required
> information from the single source of truth by using the passed ID."

> Source: Ian Lake (Google), medium.com/androiddevelopers/navigation-compose-meet-type-safety
>
> "Note: this is supposed to be a speed bump: think long and hard whether an immutable,
> snapshot-in-time argument is really the source of truth for this data, or if this should
> really be an object you retrieve from a reactive source, such as a Flow exposed from a
> repository."

```kotlin
// ❌ Passer l'objet entier en navigation
navController.navigate(UserScreen(user = fullUserObject))

// ✅ Passer seulement l'ID, charger depuis le repo dans le ViewModel
@Serializable
data class UserScreen(val userId: String)

class UserViewModel(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<UserScreen>()
    val user: Flow<User> = userRepository.getUser(route.userId)
}
```

### 2.3 Récupérer les arguments dans le ViewModel

> Source: Don Turner (Google), medium.com/androiddevelopers/type-safe-navigation-for-compose
>
> "If you're using a ViewModel to provide state to your screen, you can also obtain the route
> from savedStateHandle using the toRoute extension function."

```kotlin
class ProductViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val product: Product = savedStateHandle.toRoute()
    // utiliser product.id pour charger les données
}
```

### 2.4 Organiser les routes en nested graphs

```kotlin
// ✅ Séparer les features en extension functions sur NavGraphBuilder
fun NavGraphBuilder.authGraph(navController: NavController) {
    composable<Routes.Login> {
        LoginScreen(onSuccess = { navController.navigate(Routes.Home) })
    }
    composable<Routes.Register> { ... }
}

fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable<Routes.Home> { ... }
    composable<Routes.Profile> { ... }
}

// Dans l'AppNavHost
NavHost(navController, startDestination = Routes.Login) {
    authGraph(navController)
    homeGraph(navController)
}
```

### 2.5 Éviter la multi-navigation accidentelle (double tap)

```kotlin
// ✅ launchSingleTop évite de dupliquer la destination dans la backstack
navController.navigate(Routes.Detail(id)) {
    launchSingleTop = true
}

// ✅ Pour bottom nav — restaurer l'état à la re-sélection
navController.navigate(topLevelRoute) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

---

## 3. ASSETS — IMAGES, SVG, FONTS

### 3.1 Structure des ressources CMP

> Source: jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources.html
>
> "Compose Multiplatform provides a special compose-multiplatform-resources library and
> Gradle plugin support for accessing resources in common code across all supported platforms."

```
commonMain/
  composeResources/
    drawable/          ← PNG, WebP, XML vectors (Android) + SVG (non-Android)
    font/              ← .ttf, .otf
    values/
      strings.xml
```

**Toujours accéder aux ressources via les accesseurs générés :**
```kotlin
// ✅ Compile-safe, généré automatiquement
Image(painterResource(Res.drawable.my_icon), contentDescription = null)
Text(stringResource(Res.string.app_name))
```

### 3.2 SVG — règles par plateforme

> Source: jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources-usage.html
>
> "SVG images are supported on all platforms except Android. To access drawable resources as
> Painter images, use the painterResource() function [...] painterResource() loads either a
> BitmapPainter for rasterized image formats, such as .png, .jpg, .bmp, .webp, or a
> VectorPainter for the Android XML vector drawable format."

**Règle SVG en CMP :**
- Sur **Android** → convertir les SVG en **XML Vector Drawable** (via Android Studio ou svg2vector)
- Sur **Desktop / iOS / Web** → SVG natif supporté via `painterResource()` ou `decodeToSvgPainter()`
- Pour une icône unique multiplatform → **convertir en `ImageVector` Kotlin** (recommandé)

```kotlin
// Convertir un SVG en ImageVector Kotlin → fonctionne partout
// Outil recommandé : androidsvg, Compose Icons generator, ou IDEA plugin

// Accès SVG desktop uniquement (hors Android)
val svgPainter = remember(density) {
    Res.readBytes("files/icon.svg").decodeToSvgPainter(density)
}
Image(svgPainter, contentDescription = null)
```

### 3.3 Images réseau — Coil 3 (CMP)

> Source: coil-kt.github.io/coil/compose/
>
> "AsyncImage is a composable that executes an image request asynchronously and renders the
> result. [...] Prefer using AsyncImage in most cases. It correctly determines the size your
> image should be loaded at based on the constraints of the composable and the provided
> ContentScale."

> Source: coil-kt.github.io/coil/compose/ — sur SubcomposeAsyncImage
>
> "Subcomposition is slower than regular composition so this composable may not be suitable
> for performance-critical parts of your UI (e.g. LazyList)."

```toml
# libs.versions.toml
coil3 = "3.4.0"
coil3-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil3" }
coil3-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil3" }
```

```kotlin
// ✅ AsyncImage — à utiliser dans la majorité des cas
AsyncImage(
    model = ImageRequest.Builder(LocalPlatformContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    placeholder = painterResource(Res.drawable.placeholder),
    error = painterResource(Res.drawable.error),
    modifier = Modifier.fillMaxWidth()
)

// ❌ SubcomposeAsyncImage dans un LazyColumn — trop lent
// ✅ Préférer rememberAsyncImagePainter + Image pour les listes
val painter = rememberAsyncImagePainter(url)
Image(painter, contentDescription = null)
```

> Source: coil-kt.github.io/coil/getting_started/ — conseil pour les librairies
>
> "If you are writing a library that depends on Coil you should NOT get/set the singleton
> ImageLoader. Instead, you should depend on io.coil-kt.coil3:coil-core, create your own
> ImageLoader, and pass it around manually."

**Règle : dans une librairie, ne jamais utiliser le singleton ImageLoader de Coil.**

### 3.4 Fonts custom

```kotlin
// Dans commonMain/composeResources/font/
// → my_font_regular.ttf, my_font_bold.ttf

// Chargement
val myFontFamily = FontFamily(
    Font(Res.font.my_font_regular, weight = FontWeight.Normal),
    Font(Res.font.my_font_bold, weight = FontWeight.Bold)
)

// Dans le theme
MaterialTheme(
    typography = Typography(
        bodyLarge = TextStyle(fontFamily = myFontFamily)
    )
) { ... }
```

**Règle : toujours déclarer les fonts dans le theme, jamais inline dans un composable.**

### 3.5 Images locales haute résolution

```kotlin
// ✅ Qualifier les drawables par densité pour Android
// drawable-hdpi/, drawable-xhdpi/, etc.

// ✅ Pour Desktop — une seule image haute résolution suffit (Skia gère le scaling)

// ✅ Préférer WebP à PNG pour les photos (même qualité, ~30% plus léger)

// ❌ Ne jamais charger une image bitmap full-size dans un composable petit
// → Toujours spécifier la taille cible dans la request Coil
AsyncImage(
    model = ImageRequest.Builder(LocalPlatformContext.current)
        .data(url)
        .size(200, 200) // évite d'allouer un bitmap 4K pour une vignette
        .build(),
    contentDescription = null
)
```

---

## 4. PERFORMANCE — RÈGLES CRITIQUES

### 4.1 Build en Release avec R8 pour mesurer

> Source: developer.android.com/develop/ui/compose/performance
>
> "Build in Release Mode with R8: Try running your app in release mode. Debug mode is useful
> for spotting many problems, but it imposes a performance cost and can make it hard to spot
> other issues."

**Ne jamais profiler en debug. Toujours en release.**

### 4.2 Baseline Profiles

> Source: developer.android.com/develop/ui/compose/performance
>
> "Use Baseline Profiles: Baseline Profiles improve performance by precompiling code for
> critical user journeys. Compose includes a default profile, but ideally, you should create
> an app-specific one as well."

```kotlin
// build.gradle.kts
plugins {
    id("androidx.baselineprofile")
}
// Générer avec : ./gradlew generateBaselineProfile
```

### 4.3 Pausable composition — activé par défaut depuis BOM 2025.12.00

> Source: android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html
>
> "Pausable composition in lazy prefetch is now enabled by default. This is a fundamental
> change to how the Compose runtime schedules work, designed to significantly reduce jank
> during heavy UI workloads. Previously, once a composition started, it had to run to
> completion. If a composition was complex, this could block the main thread for longer than
> a single frame, causing the UI to freeze."

**En 2026 avec BOM 2025.12.00+, les listes lourdes sont automatiquement plus fluides.**
Rien à faire de spécial, mais il faut être sur la dernière BOM.

### 4.4 Diagnostiquer les recompositions — outils

> Source: developer.android.com/develop/ui/compose/performance/stability/diagnose
>
> "The Layout Inspector in Android Studio lets you see which composables are recomposing in
> your app. It displays counts of how many times Compose has recomposed or skipped a component."

> Source: developer.android.com/develop/ui/compose/performance/stability/diagnose
>
> "The Compose compiler can output the results of its stability inference for inspection.
> Using this output, you can determine which of your composables are skippable, and which
> are not."

**Activer les compiler reports :**
```kotlin
// build.gradle.kts
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}
```

Puis lancer `./gradlew assembleRelease` et inspecter `build/compose_compiler/`.
Un composable marqué `restartable skippable` est optimisé. `restartable` seul = problème.

### 4.5 Avoid expensive work in composables

```kotlin
// ❌ Calcul coûteux dans la composition — s'exécute à chaque recomposition
@Composable
fun Screen(data: List<Item>) {
    val processed = data.filter { it.active }.sortedBy { it.name } // coûteux
    LazyColumn { items(processed) { ItemRow(it) } }
}

// ✅ remember avec clé — recalcul uniquement si data change
@Composable
fun Screen(data: List<Item>) {
    val processed = remember(data) {
        data.filter { it.active }.sortedBy { it.name }
    }
    LazyColumn { items(processed) { ItemRow(it) } }
}

// ✅✅ Mieux : dans le ViewModel, exposé comme StateFlow
```

---

## 5. RÈGLES À NE JAMAIS VIOLER (anti-crash)

### 5.1 Ne jamais écrire un State dans la composition

→ Voir section 1.7. Cause une boucle infinie de recompositions.

### 5.2 Ne jamais lancer une coroutine dans un composable sans LaunchedEffect

```kotlin
// ❌ Coroutine lancée à chaque recomposition
@Composable
fun Bad() {
    viewModel.load() // si load() lance une coroutine → appelé à chaque recomposition
}

// ✅
@Composable
fun Good() {
    LaunchedEffect(Unit) {
        viewModel.load()
    }
}
```

### 5.3 Ne jamais utiliser GlobalScope dans du code Compose/ViewModel

```kotlin
// ❌
GlobalScope.launch { ... }

// ✅ Dans un ViewModel
viewModelScope.launch { ... }

// ✅ Dans un composable
val scope = rememberCoroutineScope()
Button(onClick = { scope.launch { ... } }) { ... }
```

### 5.4 rememberSaveable vs remember

```kotlin
// remember → survit aux recompositions, PAS aux rotations/process death
var count by remember { mutableStateOf(0) }

// rememberSaveable → survit aux rotations et au process death
var name by rememberSaveable { mutableStateOf("") }

// Règle : tout ce que l'utilisateur a saisi = rememberSaveable
// Règle : état purement UI temporaire (hover, expansion) = remember
```

### 5.5 @Composable ne doit pas retourner une valeur

```kotlin
// ❌ Anti-pattern — un composable ne doit pas retourner une valeur
@Composable
fun getTitle(): String { ... }

// ✅ Utiliser une fonction normale ou un ViewModel
fun getTitle(): String { ... }

// ✅ Ou si besoin d'état réactif
@Composable
fun Title() { Text(remember { computeTitle() }) }
```

### 5.6 Ne jamais mettre de logique business dans un composable

```kotlin
// ❌
@Composable
fun CheckoutScreen() {
    val result = repository.checkout() // logique business dans l'UI
}

// ✅ Tout dans le ViewModel, l'UI observe et dispatche des events
@Composable
fun CheckoutScreen(viewModel: CheckoutViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Button(onClick = viewModel::checkout) { Text("Payer") }
}
```

---

## 6. ARCHITECTURE — RAPPELS CLÉS

### 6.1 UiState — sealed class ou data class

```kotlin
// ✅ Pattern recommandé par Google (Now in Android)
data class HomeUiState(
    val items: ImmutableList<Item> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Dans le ViewModel
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
```

### 6.2 collectAsStateWithLifecycle vs collectAsState

```kotlin
// ❌ collectAsState — continue à collecter en background même quand l'app est en fond
val state by viewModel.uiState.collectAsState()

// ✅ collectAsStateWithLifecycle — respecte le lifecycle, meilleure perf batterie
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

Dépendance requise :
```toml
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
```

### 6.3 Hoist le state aussi haut que nécessaire, mais pas plus

```kotlin
// Règle : le state doit vivre au niveau le plus bas de l'arbre qui en a besoin

// Si uniquement un composable l'utilise → remember local
// Si plusieurs composables l'utilisent → hoist au parent commun
// Si survit à la navigation → ViewModel
// Si survit au process death → rememberSaveable ou persistence
```

---

## 7. VÉRIFICATIONS AVANT CHAQUE PR

- [ ] Pas de `List<>` brute dans les paramètres de composables → `ImmutableList` ou wrapper `@Immutable`
- [ ] Pas de lecture de state dans le scope parent si elle peut être différée dans un child
- [ ] Modifier animés → version lambda (`offset { }`, `graphicsLayer { }`)
- [ ] `derivedStateOf` pour tout state qui change très souvent (scroll, animation)
- [ ] Keys stables sur tous les `LazyColumn`/`LazyRow`
- [ ] Calculs coûteux dans `remember(key) { }` ou dans le ViewModel
- [ ] Aucun `GlobalScope`, aucune coroutine directe dans un composable
- [ ] Routes de navigation = `@Serializable data class`, pas de strings
- [ ] SVG Android = XML Vector Drawable (pas .svg brut)
- [ ] Images réseau = Coil3 `AsyncImage` (pas `SubcomposeAsyncImage` dans LazyList)
- [ ] `collectAsStateWithLifecycle()` et non `collectAsState()`
- [ ] Profiling en **release**, pas en debug

---

## 8. SOURCES DE RÉFÉRENCE

| Sujet | Source officielle |
|---|---|
| Performance Compose | developer.android.com/develop/ui/compose/performance |
| Stabilité & diagnostics | developer.android.com/develop/ui/compose/performance/stability |
| Phases Compose | developer.android.com/develop/ui/compose/phases |
| Strong Skipping | developer.android.com/develop/ui/compose/performance/stability/strongskipping |
| Navigation type-safe | developer.android.com/guide/navigation/design/type-safety |
| Ressources CMP | jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources.html |
| Coil 3 | coil-kt.github.io/coil/compose/ |
| Nouveautés Compose déc. 2025 | android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html |
| Ian Lake — type-safe nav | medium.com/androiddevelopers/navigation-compose-meet-type-safety |
| Ben Trengrove — debugging recomposition | medium.com/androiddevelopers/jetpack-compose-debugging-recomposition |
| Ben Trengrove — strong skipping | medium.com/androiddevelopers/jetpack-compose-strong-skipping-mode-explained |
| Jorge Castillo — collections & strong skipping | newsletter.jorgecastillo.dev |