package wtf.the.spring.session.datastore;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Blob;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static org.springframework.util.SerializationUtils.serialize;

public class FirestoreSessionRepository implements SessionRepository<MapSession> {

    private final CollectionReference collection;
    private final Duration ttl;

    public FirestoreSessionRepository(CollectionReference collection, Duration ttl) {
        this.collection = requireNonNull(collection);
        this.ttl = requireNonNull(ttl);
    }

    @Override
    public MapSession createSession() {
        var session = new MapSession();
        session.setMaxInactiveInterval(ttl);
        return session;
    }

    @Override
    public void save(MapSession session) {
        try {
            collection
                .document(session.getId())
                .set(Map.of(
                    "data", Blob.fromBytes(requireNonNull(serialize(session))),
                    "expire", Timestamp.of(Date.from(session.getLastAccessedTime().plus(session.getMaxInactiveInterval())))
                ))
                .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MapSession findById(String id) {
        try {
            return Optional
                .of(collection.document(id).get().get())
                .filter(DocumentSnapshot::exists)
                .map(entity -> entity.getBlob("data"))
                .map(Blob::toBytes)
                .map(SerializationUtils::deserialize)
                .filter(MapSession.class::isInstance)
                .map(MapSession.class::cast)
                .filter(not(Session::isExpired))
                .orElse(null);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            collection.document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
