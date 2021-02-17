package wtf.the.spring.session.datastore;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;

import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;

public class FirestoreSessionCleaner {

    private final CollectionReference collection;

    public FirestoreSessionCleaner(CollectionReference collection) {
        this.collection = requireNonNull(collection);
    }

    public void clean() {
        try {
            while (true) {
                var batch = collection.getFirestore().batch();
                collection
                    .whereLessThan("expire", Timestamp.now())
                    .limit(500)
                    .select(new String[]{})
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(QueryDocumentSnapshot::getReference)
                    .forEach(batch::delete);
                if (batch.commit().get().isEmpty()) {
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
