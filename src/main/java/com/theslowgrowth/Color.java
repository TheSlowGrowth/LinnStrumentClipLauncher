package com.theslowgrowth;

/**
 * Represents Linnstruments limited colour pallette
 */
public enum Color {
    OFF(0),
    RED(1),
    YELLOW(2),
    GREEN(3),
    CYAN(4),
    BLUE(5),
    MAGENTA(6),
    BLACK(7),
    WHITE(8),
    ORANGE(9),
    LIME(10),
    PINK(11);

    Color(int value) {
        value_ = value;
    }

    public int getValue() {
        return value_;
    }

    /** convert from RGB colours
     * @param r     red value
     * @param g     green value
     * @param b     blue value
     */
    public static Color fromRGB(float r, float g, float b) {
        float maximum = Math.max(r, Math.max(g, b));
        float minimum = Math.min(r, Math.min(g, b));
        float delta = maximum - minimum;

        if (maximum < 0.1)
            return BLACK;
        else if (delta <= 0.2) // very little saturation
            return WHITE;
        else
        {
            float h = 0;
            if (r == maximum)
                h = 60 * ((g-b)/delta);
            else if (g == maximum)
                h = 60 * (((b-r)/delta) + 2);
            else
                h = 60 * (((r-g)/delta) + 4);
            if (h < 0)
                h += 360;
            // select colour:
            if (h > 340)
                return RED;
            else if (h > 300)
                return MAGENTA;
            else if (h > 270)
                return PINK;
            else if (h > 210)
                return BLUE;
            else if (h > 160)
                return CYAN;
            else if (h > 100)
                return GREEN;
            else if (h > 70)
                return LIME;
            else if (h > 50)
                return YELLOW;
            else if (h > 20)
                return ORANGE;
            else
                return RED;
        }
    }

    private int value_;
}
