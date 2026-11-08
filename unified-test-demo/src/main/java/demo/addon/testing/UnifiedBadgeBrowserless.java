package demo.addon.testing;

import demo.addon.Badge;

/** {@link UnifiedBadge} on Browserless: the server-side component through the Escape Hatch. */
public interface UnifiedBadgeBrowserless extends UnifiedBadge {

    @Override
    default int getCount() {
        return unwrap(Badge.class).getCount();
    }
}
