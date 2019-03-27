package map.content.chest;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class RandomInstance<T> {
    private Random random;
    private List<Supplier<T>> suppliers;

    @SafeVarargs
    public RandomInstance(Random random, Supplier<T>... rands) {
        this.random = random;
        suppliers = Arrays.asList(rands);
    }

    public T instance(int... percents) {
        assert percents.length == suppliers.size();
        double rand = this.random.nextDouble();
        for (int i = 0; i < percents.length; i++) {
            if (rand <= percents[i] / 100D)
                return suppliers.get(i).get();
        }
        return null;
    }
}
