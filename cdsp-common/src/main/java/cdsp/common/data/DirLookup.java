/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data;

import java.io.File;

/**
 * Directory lookup device.
 */
public interface DirLookup {
    public File findFile(Location location, String name);
    public static enum Location {
        BACKGROUNDS("Backgrounds", "Backgrounds", false),
        BODY_DATA("Body Data", "Body Data", false),
        BOOTSTRAP("Bootstrap", "Bootstrap", false),
        CATALOGUE("Catalogue", "Catalogue", false),
        CREATURE_DATABASE("Creature Database", "Creature Galleries", false),
        EXPORTED_CREATURES("Exported Creatures", "My Creatures", false),
        GENETICS("Genetics", "Genetics", true),
        IMAGES("Images", "Images", true),
        JOURNAL("Journal", "Journal", true),
        MAIN("Main", ".", false),
        OVERLAY_DATA("Overlay Data", "Overlay Data", false),
        RESOURCE_FILES("Resource Files", "My Agents", false),
        SOUNDS("Sounds", "Sounds", false),
        USERS("Users", "Users", false),
        WORLDS("Worlds", "My Worlds", false);

        public final String nameInternal, nameTypical;
        public final boolean hasWorld;

        Location(String nameInternal, String nameTypical, boolean hasWorld) {
            this.nameInternal = nameInternal;
            this.nameTypical = nameTypical;
            this.hasWorld = hasWorld;
        }
    }
}
