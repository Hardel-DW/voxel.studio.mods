
> Task :compileClientJava FAILED

[Incubating] Problems report is available at: file:///C:/Users/Hardel/Desktop/repository/asset_editor/build/reports/problems/problems-report.html
<====---------> 30% EXECUTING [4s]
> IDLE
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:31: error: cannot find symbol
    public static Map<String, Identifier> slotFolderIcons() {
                              ^
  symbol:   class Identifier
  location: class EnchantmentTreeBuilder
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:39: error: cannot find symbol
    public static Map<String, Identifier> itemFolderIcons(int version) {
                              ^
  symbol:   class Identifier
  location: class EnchantmentTreeBuilder
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentOverviewRow.java:37: error: cannot find symbol
        var previewTexture = EnchantmentResolver.previewTexture(enchantment);
                             ^
  symbol:   variable EnchantmentResolver
  location: class EnchantmentOverviewRow
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentOverviewRow.java:47: error: cannot find symbol
        Label name = new Label(EnchantmentResolver.enchantmentDisplayName(enchantment));
                               ^
  symbol:   variable EnchantmentResolver
  location: class EnchantmentOverviewRow
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentOverviewRow.java:73: error: cannot find symbol
        if (!EnchantmentResolver.isVanilla(enchantment)) {
             ^
  symbol:   variable EnchantmentResolver
  location: class EnchantmentOverviewRow
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:32: error: cannot find symbol
        LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
                              ^
  symbol:   class Identifier
  location: class EnchantmentTreeBuilder
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:40: error: cannot find symbol
        LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
                              ^
  symbol:   class Identifier
  location: class EnchantmentTreeBuilder
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:70: error: cannot find symbol
                if (EnchantmentResolver.matchesItemReference(enchantment, tag.key())) {
                    ^
  symbol:   variable EnchantmentResolver
  location: class EnchantmentTreeBuilder
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:111: error: cannot find symbol
        return EnchantmentResolver.enchantmentDisplayName(enchantment);
               ^
  symbol:   variable EnchantmentResolver
  location: class EnchantmentTreeBuilder
C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\routes\enchantment\EnchantmentOverviewPage.java:77: error: constructor EnchantmentOverviewCard in class EnchantmentOverviewCard cannot be applied to given types;
            grid.addItem(new EnchantmentOverviewCard(enchantment, () -> open(enchantment)));
                         ^
  required: StudioMockEnchantment,String,Identifier,boolean,Runnable
  found:    StudioMockEnchantment,()->open(e[...]ment)
  reason: actual and formal argument lists differ in length
10 errors

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileClientJava'.
> Compilation failed; see the compiler output below.
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\routes\enchantment\EnchantmentOverviewPage.java:77: error: constructor EnchantmentOverviewCard in class EnchantmentOverviewCard cannot be applied to given types;
              grid.addItem(new EnchantmentOverviewCard(enchantment, () -> open(enchantment)));
                           ^
    required: StudioMockEnchantment,String,Identifier,boolean,Runnable
    found:    StudioMockEnchantment,()->open(e[...]ment)
    reason: actual and formal argument lists differ in length
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:31: error: cannot find symbol
      public static Map<String, Identifier> slotFolderIcons() {
                                ^
    symbol:   class Identifier
    location: class EnchantmentTreeBuilder
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:39: error: cannot find symbol
      public static Map<String, Identifier> itemFolderIcons(int version) {
                                ^
    symbol:   class Identifier
    location: class EnchantmentTreeBuilder
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentOverviewRow.java:37: error: cannot find symbol
          var previewTexture = EnchantmentResolver.previewTexture(enchantment);
                               ^
    symbol:   variable EnchantmentResolver
    location: class EnchantmentOverviewRow
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentOverviewRow.java:47: error: cannot find symbol
          Label name = new Label(EnchantmentResolver.enchantmentDisplayName(enchantment));
                                 ^
    symbol:   variable EnchantmentResolver
    location: class EnchantmentOverviewRow
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentOverviewRow.java:73: error: cannot find symbol
          if (!EnchantmentResolver.isVanilla(enchantment)) {
               ^
    symbol:   variable EnchantmentResolver
    location: class EnchantmentOverviewRow
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:32: error: cannot find symbol
          LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
                                ^
    symbol:   class Identifier
    location: class EnchantmentTreeBuilder
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:40: error: cannot find symbol
          LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
                                ^
    symbol:   class Identifier
    location: class EnchantmentTreeBuilder
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:70: error: cannot find symbol
                  if (EnchantmentResolver.matchesItemReference(enchantment, tag.key())) {
                      ^
    symbol:   variable EnchantmentResolver
    location: class EnchantmentTreeBuilder
  C:\Users\Hardel\Desktop\repository\asset_editor\src\client\java\fr\hardel\asset_editor\client\javafx\components\page\enchantment\EnchantmentTreeBuilder.java:111: error: cannot find symbol
          return EnchantmentResolver.enchantmentDisplayName(enchantment);
                 ^
    symbol:   variable EnchantmentResolver
    location: class EnchantmentTreeBuilder
  10 errors