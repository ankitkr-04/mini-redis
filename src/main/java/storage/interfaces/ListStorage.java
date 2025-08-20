package storage.interfaces;


import java.util.List;
import java.util.Optional;

public interface ListStorage {
    int leftPush(String key, String... values);

    int rightPush(String key, String... values);

    Optional<String> leftPop(String key);

    Optional<String> rightPop(String key);

    List<String> leftPop(String key, int count);

    List<String> rightPop(String key, int count);

    List<String> getListRange(String key, int start, int end);

    int getListLength(String key);
}
