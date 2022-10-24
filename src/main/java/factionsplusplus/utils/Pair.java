package factionsplusplus.utils;

public class Pair<F, S>
{
    private final F first;
    private final S second;

    private Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F left() {
        return this.first;
    }

    public S right() {
        return this.second;
    }

    public static <F, S> Pair <F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }
}
