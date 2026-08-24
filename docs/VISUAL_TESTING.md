# Visual regression testing

## Coverage and comparison

The browser suite captures each portal at five supported responsive sizes after disabling animations
and transitions. Versioned PNG baselines are compared by dimensions and pixels. A pixel is different
when any color channel differs by more than 12; the default allowed ratio is 0.001.

## Review workflow

Unexpected changes publish the current screenshot and a magenta diff mask. A baseline may be updated
only for an intentional, reviewed design change. Review checks layout hierarchy, clipping, overflow,
focus visibility, typography, color, responsive reflow, and consistency across all three portals.

Font, browser, operating-system, viewport, locale, theme, data, and motion settings must remain fixed
inside a comparison lane. Cross-platform rendering uses separate baseline lanes rather than raising
the global tolerance.
