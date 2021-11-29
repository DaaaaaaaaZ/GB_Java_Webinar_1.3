package dz.webinar1_3;

public abstract class Fruit <T> {
    private final float weight;
    private final String title;

    public Fruit (String title, float weight) {
        this.title = title;
        this.weight = weight;
    }

    public float getWeight () {
        return weight;
    }
}
