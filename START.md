Hey! We're going to develop a mod that is an asset editor in Minecraft. An interface accessible from the creative menu that allows creating/modifying datapacks/resourcepacks from a zip or jar. If it's a folder we can edit it. If it's a zip/jar we must override it, meaning create a pack that modifies the file. So we don't modify the zip and jar directly.

For this, we'll base it on a UI lib, JavaFX.
And we'll display this in I think a separate application. If it's possible otherwise too bad we'll do it in-game.

And we also base it on the code I had made for the web version, because this project has already been done.
We literally copy the UI I made in web. The layout, images, svg, animations.

We start simple at the beginning. We only handle the current version 1.21.11 which simplifies everything.
We'll try to see for a reactive approach.

We'll first start with the editor globally, then just the enchantment. Need to rewrite in Java things I've written without taking initiative.
We can simplify enormously by plugging into Minecraft. The registries/codecs and Mojang's methods as well as their renders.
For the editor here's how it is:
The visual design is heavily based on rounded elements. A bar at the top displays the selected tab. The app logo is positioned in the top left corner. The concepts are located on the left side. Settings are placed in the bottom left corner.

We have a second bar on the left with the tree structure and the component tree.
And after the view.
You must explore the code always reference the code/file/line of the initial project. And ask me when you want to differ from the initial project.
No simplification of my original code you must talk to me about it obligatorily.

We're not looking to go fast, we're looking

Order of work :
1. Read the code of the initial project.
2. First, we'll create the visual for the generic editor and Migrate images, relevant svg.
3. We'll do the Enchantment page first, and the General page will be the first test vector. Added the 5 other tabs but empty.
4 Add the tree structure to the sidebar.
5. And after that, when everything is up and running, we'll do the rest.