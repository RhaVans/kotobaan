package com.kotoba.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.kotoba.app.data.model.Chapter;
import com.kotoba.app.data.model.CycleItem;
import com.kotoba.app.data.model.LearningCycle;
import com.kotoba.app.data.model.LearningObject;
import com.kotoba.app.data.model.UserProgress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KotobaDatabase extends SQLiteOpenHelper {
    private static final String TAG = "KotobaDatabase";
    private static final String DATABASE_NAME = "kotoba.db";
    private static final int DATABASE_VERSION = 3;

    private static final String LO_COLUMNS =
            "id, type, bab, level, japanese, reading, romaji, indonesian, badge_label, group_label, sort_order, details_json, word_type";

    private static KotobaDatabase sInstance;
    private final Context mContext;
    private final String mDbPath;

    public static synchronized KotobaDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KotobaDatabase(context.getApplicationContext());
        }
        return sInstance;
    }

    private KotobaDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
        this.mDbPath = context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
        ensureDatabaseCopied();
    }

    private void ensureDatabaseCopied() {
        File dbFile = new File(mDbPath);
        boolean needsDeploy = false;

        if (!dbFile.exists()) {
            needsDeploy = true;
        } else {
            SQLiteDatabase checkDb = null;
            try {
                checkDb = SQLiteDatabase.openDatabase(mDbPath, null, SQLiteDatabase.OPEN_READONLY);
                int userVersion = checkDb.getVersion();
                if (userVersion < 3) {
                    needsDeploy = true;
                    Log.i(TAG, "Existing database is older version (" + userVersion + " < 3), redeploying clean Indonesian database...");
                } else {
                    // Verify if the deployed database contains stale English definitions or lacks Additional Kanji
                    Cursor cAdd = checkDb.rawQuery("SELECT count(*) FROM learning_objects WHERE id LIKE 'kanji_add_%'", null);
                    if (cAdd != null) {
                        if (cAdd.moveToFirst() && cAdd.getInt(0) < 2119) {
                            needsDeploy = true;
                            Log.i(TAG, "Existing database lacks Additional Kanji, redeploying...");
                        }
                        cAdd.close();
                    }

                    if (!needsDeploy) {
                        Cursor cEng = checkDb.rawQuery(
                                "SELECT count(*) FROM learning_objects WHERE indonesian LIKE '%residence%' OR indonesian LIKE '%assurance%'",
                                null
                        );
                        if (cEng != null) {
                            if (cEng.moveToFirst() && cEng.getInt(0) > 0) {
                                needsDeploy = true;
                                Log.i(TAG, "Existing database contains stale English glosses, redeploying clean Indonesian database...");
                            }
                            cEng.close();
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error checking existing database, will redeploy", e);
                needsDeploy = true;
            } finally {
                if (checkDb != null && checkDb.isOpen()) {
                    checkDb.close();
                }
            }
        }

        if (needsDeploy) {
            deployAssetDatabase(dbFile);
        }
    }

    private void deployAssetDatabase(File dbFile) {
        // 1. If database exists, extract user progress to preserve it across updates
        List<UserProgress> savedProgress = new ArrayList<>();
        if (dbFile.exists()) {
            SQLiteDatabase oldDb = null;
            try {
                oldDb = SQLiteDatabase.openDatabase(mDbPath, null, SQLiteDatabase.OPEN_READONLY);
                Cursor c = oldDb.rawQuery(
                        "SELECT object_id, interval_days, ease_factor, repetitions, lapses, stability, retrievability, last_response_time_ms, last_review_epoch, next_review_epoch, mastery_state " +
                        "FROM user_progress WHERE repetitions > 0 OR mastery_state != 'UNSEEN'",
                        null
                );
                if (c != null) {
                    while (c.moveToNext()) {
                        savedProgress.add(cursorToUserProgress(c));
                    }
                    c.close();
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not extract previous user progress", e);
            } finally {
                if (oldDb != null && oldDb.isOpen()) {
                    oldDb.close();
                }
            }
        }

        // 2. Ensure parent directory exists
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // 3. Copy latest pre-compiled database from assets
        try {
            Log.i(TAG, "Deploying fresh kotoba.db from assets...");
            InputStream is = mContext.getAssets().open("databases/" + DATABASE_NAME);
            OutputStream os = new FileOutputStream(dbFile);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            os.close();
            is.close();
            Log.i(TAG, "Fresh kotoba.db successfully deployed from assets.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to deploy kotoba.db from assets", e);
            return;
        }

        // 4. Restore preserved user progress
        if (!savedProgress.isEmpty()) {
            SQLiteDatabase newDb = null;
            try {
                newDb = SQLiteDatabase.openDatabase(mDbPath, null, SQLiteDatabase.OPEN_READWRITE);
                newDb.beginTransaction();
                for (UserProgress up : savedProgress) {
                    ContentValues cv = new ContentValues();
                    cv.put("interval_days", up.getIntervalDays());
                    cv.put("ease_factor", up.getEaseFactor());
                    cv.put("repetitions", up.getRepetitions());
                    cv.put("lapses", up.getLapses());
                    cv.put("stability", up.getStability());
                    cv.put("retrievability", up.getRetrievability());
                    cv.put("last_response_time_ms", up.getLastResponseTimeMs());
                    cv.put("last_review_epoch", up.getLastReviewEpoch());
                    cv.put("next_review_epoch", up.getNextReviewEpoch());
                    cv.put("mastery_state", up.getMasteryState().name());
                    newDb.update("user_progress", cv, "object_id = ?", new String[]{up.getObjectId()});
                }
                newDb.setTransactionSuccessful();
                Log.i(TAG, "Restored " + savedProgress.size() + " user progress records into updated database.");
            } catch (Exception e) {
                Log.w(TAG, "Error restoring user progress", e);
            } finally {
                if (newDb != null) {
                    if (newDb.inTransaction()) newDb.endTransaction();
                    if (newDb.isOpen()) newDb.close();
                }
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Database is pre-compiled and copied from assets.
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Database upgrade requested from " + oldVersion + " to " + newVersion);
        // ensureDatabaseCopied verifies dataset completeness before connection opens
    }

    public List<Chapter> getAllChapters() {
        List<Chapter> chapters = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT bab_number, title_ja, title_id, theme, level, vocab_count, grammar_count FROM chapters ORDER BY bab_number ASC", null);
        if (c != null) {
            while (c.moveToNext()) {
                chapters.add(new Chapter(
                        c.getInt(0),
                        c.getString(1),
                        c.getString(2),
                        c.getString(3),
                        c.getString(4),
                        c.getInt(5),
                        c.getInt(6)
                ));
            }
            c.close();
        }
        return chapters;
    }

    public Chapter getChapter(int bab) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT bab_number, title_ja, title_id, theme, level, vocab_count, grammar_count FROM chapters WHERE bab_number = ?", new String[]{String.valueOf(bab)});
        Chapter chapter = null;
        if (c != null) {
            if (c.moveToFirst()) {
                chapter = new Chapter(
                        c.getInt(0),
                        c.getString(1),
                        c.getString(2),
                        c.getString(3),
                        c.getString(4),
                        c.getInt(5),
                        c.getInt(6)
                );
            }
            c.close();
        }
        return chapter;
    }

    public List<LearningObject> getVocabularyForBab(int bab) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = 'VOCABULARY' AND bab = ? ORDER BY sort_order ASC",
                new String[]{String.valueOf(bab)}
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> getVocabularyForBabs(Collection<Integer> babs) {
        if (babs == null || babs.isEmpty()) {
            return getAllChapterVocabulary();
        }
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(LO_COLUMNS).append(" FROM learning_objects WHERE type = 'VOCABULARY' AND bab IN (");
        String[] args = new String[babs.size()];
        int i = 0;
        for (Integer b : babs) {
            if (i > 0) sb.append(",");
            sb.append("?");
            args[i++] = String.valueOf(b);
        }
        sb.append(") ORDER BY bab ASC, sort_order ASC");
        Cursor c = db.rawQuery(sb.toString(), args);
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> getAllChapterVocabulary() {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = 'VOCABULARY' AND bab > 0 ORDER BY bab ASC, sort_order ASC",
                null
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> getLearningObjectsByIds(List<String> ids) {
        List<LearningObject> list = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return list;
        SQLiteDatabase db = getReadableDatabase();
        Map<String, LearningObject> map = new HashMap<>();
        int chunkSize = 500;
        for (int i = 0; i < ids.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, ids.size());
            List<String> sub = ids.subList(i, end);
            StringBuilder sb = new StringBuilder("SELECT ").append(LO_COLUMNS).append(" FROM learning_objects WHERE id IN (");
            String[] args = new String[sub.size()];
            for (int j = 0; j < sub.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append("?");
                args[j] = sub.get(j);
            }
            sb.append(")");
            Cursor c = db.rawQuery(sb.toString(), args);
            if (c != null) {
                while (c.moveToNext()) {
                    LearningObject obj = cursorToLearningObject(c);
                    map.put(obj.getId(), obj);
                }
                c.close();
            }
        }
        for (String id : ids) {
            LearningObject obj = map.get(id);
            if (obj != null) {
                list.add(obj);
            }
        }
        return list;
    }

    public List<LearningObject> getAllKanji() {
        return getFormalKanji();
    }

    public List<LearningObject> getFormalKanji() {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%' ORDER BY sort_order ASC",
                null
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> getAdditionalKanji() {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE id LIKE 'kanji_add_%' ORDER BY sort_order ASC",
                null
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<String> getFormalKanjiGroups() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT group_label FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC",
                null
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
            c.close();
        }
        return list;
    }

    public List<String> getAdditionalKanjiGroups() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT group_label FROM learning_objects WHERE id LIKE 'kanji_add_%' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC",
                null
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> getKanjiByGroup(String groupLabel) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = 'KANJI' AND group_label = ? ORDER BY sort_order ASC",
                new String[]{groupLabel}
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<String> getKanjiGroups() {
        return getFormalKanjiGroups();
    }

    public List<LearningObject> getVerbsByGroup(String groupLabel) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE word_type = 'KATA_KERJA' AND group_label = ? ORDER BY sort_order ASC",
                new String[]{groupLabel}
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<String> getVerbGroups() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT group_label FROM learning_objects WHERE word_type = 'KATA_KERJA' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC",
                null
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> getAdjectivesByGroup(String groupLabel) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE (word_type = 'KATA_SIFAT_NA' OR word_type = 'KATA_SIFAT_I') AND group_label = ? ORDER BY sort_order ASC",
                new String[]{groupLabel}
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<String> getAdjectiveGroups() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT group_label FROM learning_objects WHERE (word_type = 'KATA_SIFAT_NA' OR word_type = 'KATA_SIFAT_I') AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC",
                null
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> getGrammarForBab(int bab) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = 'GRAMMAR' AND bab = ? ORDER BY sort_order ASC",
                new String[]{String.valueOf(bab)}
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> getAllGrammar() {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = 'GRAMMAR' ORDER BY sort_order ASC",
                null
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public Map<LearningObject.WordType, Integer> getWordTypeCounts() {
        Map<LearningObject.WordType, Integer> map = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT word_type, count(*) FROM learning_objects GROUP BY word_type", null);
        if (c != null) {
            while (c.moveToNext()) {
                LearningObject.WordType wt = LearningObject.WordType.fromString(c.getString(0));
                map.put(wt, c.getInt(1));
            }
            c.close();
        }
        return map;
    }

    /**
     * Stacking filter query for Pustaka vocabulary browser (Single Bab backward compatibility).
     */
    public List<LearningObject> queryVocabulary(String wordType, Integer bab, String searchQuery, boolean onlyWeak) {
        return queryVocabulary(wordType, (bab != null && bab > 0) ? Collections.singleton(bab) : null, null, 50, searchQuery, onlyWeak);
    }

    /**
     * Stacking filter query for Pustaka vocabulary browser supporting:
     * - Multi-Bab selection (e.g. Bab 1 & 2, or Bab 4 & 25)
     * - 50-kotoba batch slicing (batchIndex 1, 2, ... or null for all)
     */
    public List<LearningObject> queryVocabulary(String wordType, Collection<Integer> babs, Integer batchIndex, int batchSize, String searchQuery, boolean onlyWeak) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder sql = new StringBuilder();
        List<String> args = new ArrayList<>();

        sql.append("SELECT lo.id, lo.type, lo.bab, lo.level, lo.japanese, lo.reading, lo.romaji, lo.indonesian, ")
           .append("lo.badge_label, lo.group_label, lo.sort_order, lo.details_json, lo.word_type ")
           .append("FROM learning_objects lo ");

        if (onlyWeak) {
            sql.append("INNER JOIN user_progress up ON lo.id = up.object_id ");
        }

        sql.append("WHERE lo.type = 'VOCABULARY' ");

        if (onlyWeak) {
            sql.append("AND (up.mastery_state = 'WEAK' OR up.lapses > 0) ");
        }

        if (wordType != null && !wordType.isEmpty() && !wordType.equalsIgnoreCase("SEMUA")) {
            if ("KATA_SIFAT".equalsIgnoreCase(wordType)) {
                sql.append("AND lo.word_type IN ('KATA_SIFAT_I', 'KATA_SIFAT_NA') ");
            } else {
                sql.append("AND lo.word_type = ? ");
                args.add(wordType.toUpperCase());
            }
        }

        if (babs != null && !babs.isEmpty()) {
            sql.append("AND lo.bab IN (");
            int bi = 0;
            for (Integer b : babs) {
                if (bi > 0) sql.append(",");
                sql.append("?");
                args.add(String.valueOf(b));
                bi++;
            }
            sql.append(") ");
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String q = "%" + searchQuery.trim() + "%";
            sql.append("AND (lo.japanese LIKE ? OR lo.reading LIKE ? OR lo.romaji LIKE ? OR lo.indonesian LIKE ?) ");
            args.add(q);
            args.add(q);
            args.add(q);
            args.add(q);
        }

        if (babs != null && !babs.isEmpty()) {
            sql.append("ORDER BY lo.bab ASC, lo.sort_order ASC ");
        } else {
            sql.append("ORDER BY CASE WHEN lo.id LIKE 'verb_jft_%' OR lo.id LIKE 'adj_jft_%' THEN 0 ELSE 1 END, lo.bab ASC, lo.sort_order ASC ");
        }

        if (batchIndex != null && batchIndex > 0) {
            int offset = (batchIndex - 1) * batchSize;
            sql.append("LIMIT ? OFFSET ?");
            args.add(String.valueOf(batchSize));
            args.add(String.valueOf(offset));
        }

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    /**
     * Returns the total vocabulary count for a specific word type.
     */
    public int getWordTypeCount(String wordType) {
        SQLiteDatabase db = getReadableDatabase();
        String sql;
        String[] args;
        if (wordType == null || wordType.isEmpty() || "SEMUA".equalsIgnoreCase(wordType)) {
            sql = "SELECT COUNT(*) FROM learning_objects WHERE type = 'VOCABULARY'";
            args = null;
        } else if ("KATA_SIFAT".equalsIgnoreCase(wordType)) {
            sql = "SELECT COUNT(*) FROM learning_objects WHERE type = 'VOCABULARY' AND word_type IN ('KATA_SIFAT_I', 'KATA_SIFAT_NA')";
            args = null;
        } else {
            sql = "SELECT COUNT(*) FROM learning_objects WHERE type = 'VOCABULARY' AND word_type = ?";
            args = new String[]{wordType.toUpperCase()};
        }
        Cursor c = db.rawQuery(sql, args);
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
        }
        return count;
    }

    /**
     * Stacking filter query for Pustaka Kanji browser.
     */
    public List<LearningObject> queryKanji(String subSection, String groupLabel, String searchQuery) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder sql = new StringBuilder();
        List<String> args = new ArrayList<>();

        sql.append("SELECT lo.id, lo.type, lo.bab, lo.level, lo.japanese, lo.reading, lo.romaji, lo.indonesian, ")
           .append("lo.badge_label, lo.group_label, lo.sort_order, lo.details_json, lo.word_type ")
           .append("FROM learning_objects lo WHERE lo.type = 'KANJI' ");

        if ("613".equalsIgnoreCase(subSection)) {
            sql.append("AND lo.id NOT LIKE 'kanji_add_%' ");
        } else if ("ADDITIONAL".equalsIgnoreCase(subSection) || "TAMBAHAN".equalsIgnoreCase(subSection)) {
            sql.append("AND lo.id LIKE 'kanji_add_%' ");
        }

        if (groupLabel != null && !groupLabel.isEmpty() && !groupLabel.equalsIgnoreCase("SEMUA")) {
            sql.append("AND lo.group_label = ? ");
            args.add(groupLabel);
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String q = "%" + searchQuery.trim() + "%";
            sql.append("AND (lo.japanese LIKE ? OR lo.reading LIKE ? OR lo.romaji LIKE ? OR lo.indonesian LIKE ?) ");
            args.add(q);
            args.add(q);
            args.add(q);
            args.add(q);
        }

        sql.append("ORDER BY lo.sort_order ASC");

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> queryKanji(String groupLabel, String searchQuery) {
        return queryKanji(null, groupLabel, searchQuery);
    }

    public List<LearningObject> queryLibrary(String searchQuery, String wordType, Integer bab, String groupLabel, boolean onlyWeak) {
        return queryVocabulary(wordType, bab, searchQuery, onlyWeak);
    }

    public UserProgress getUserProgress(String objectId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT object_id, interval_days, ease_factor, repetitions, lapses, stability, retrievability, last_response_time_ms, last_review_epoch, next_review_epoch, mastery_state " +
                "FROM user_progress WHERE object_id = ?",
                new String[]{objectId}
        );
        UserProgress up = null;
        if (c != null) {
            if (c.moveToFirst()) {
                up = cursorToUserProgress(c);
            }
            c.close();
        }
        return up;
    }

    public void saveUserProgress(UserProgress progress) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("object_id", progress.getObjectId());
        cv.put("interval_days", progress.getIntervalDays());
        cv.put("ease_factor", progress.getEaseFactor());
        cv.put("repetitions", progress.getRepetitions());
        cv.put("lapses", progress.getLapses());
        cv.put("stability", progress.getStability());
        cv.put("retrievability", progress.getRetrievability());
        cv.put("last_response_time_ms", progress.getLastResponseTimeMs());
        cv.put("last_review_epoch", progress.getLastReviewEpoch());
        cv.put("next_review_epoch", progress.getNextReviewEpoch());
        cv.put("mastery_state", progress.getMasteryState().name());
        db.insertWithOnConflict("user_progress", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void logReviewAttempt(String objectId, int responseTimeMs, boolean isCorrect, String errorCategory, String chosen, String correct) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("object_id", objectId);
        cv.put("timestamp_epoch", System.currentTimeMillis() / 1000L);
        cv.put("response_time_ms", responseTimeMs);
        cv.put("is_correct", isCorrect ? 1 : 0);
        cv.put("error_category", errorCategory);
        cv.put("chosen_answer", chosen);
        cv.put("correct_answer", correct);
        db.insert("review_attempts", null, cv);
    }

    public List<LearningObject> getDueReviewItems(long currentEpoch, int limit) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects lo " +
                "INNER JOIN user_progress up ON lo.id = up.object_id " +
                "WHERE up.next_review_epoch <= ? AND up.mastery_state != 'UNSEEN' " +
                "ORDER BY up.next_review_epoch ASC LIMIT ?",
                new String[]{String.valueOf(currentEpoch), String.valueOf(limit)}
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public boolean isItemWeak(String objectId) {
        if (objectId == null) return false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM user_progress WHERE object_id = ? AND (mastery_state = 'WEAK' OR lapses > 0)", new String[]{objectId});
        boolean isWeak = false;
        if (c != null) {
            isWeak = c.moveToFirst();
            c.close();
        }
        return isWeak;
    }

    public List<LearningObject> getWeakItems(int limit) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + LO_COLUMNS + " FROM learning_objects lo " +
                "INNER JOIN user_progress up ON lo.id = up.object_id " +
                "WHERE up.mastery_state = 'WEAK' OR up.lapses > 1 " +
                "ORDER BY up.stability ASC LIMIT ?",
                new String[]{String.valueOf(limit)}
        );
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public List<LearningObject> getDueReviews(long now, int limit) {
        return getDueReviewItems(now, limit);
    }

    public List<LearningObject> getWeakObjects(int limit) {
        return getWeakItems(limit);
    }

    public List<LearningObject> getRandomBatch(LearningObject.Type type, int count, Integer bab) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query;
        String[] args;
        if (bab != null && bab > 0) {
            query = "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = ? AND bab = ? ORDER BY RANDOM() LIMIT ?";
            args = new String[]{type.name(), String.valueOf(bab), String.valueOf(count)};
        } else {
            query = "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = ? ORDER BY RANDOM() LIMIT ?";
            args = new String[]{type.name(), String.valueOf(count)};
        }
        Cursor c = db.rawQuery(query, args);
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    public void updateUserProgress(UserProgress progress) {
        saveUserProgress(progress);
    }

    public void recordReviewAttempt(String objectId, long epoch, int latencyMs, boolean isCorrect, String errorCategory, String chosen, String correct) {
        logReviewAttempt(objectId, latencyMs, isCorrect, errorCategory, chosen, correct);
    }

    public Map<UserProgress.MasteryState, Integer> getMasteryCounts() {
        Map<UserProgress.MasteryState, Integer> map = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT mastery_state, count(*) FROM user_progress GROUP BY mastery_state", null);
        if (c != null) {
            while (c.moveToNext()) {
                try {
                    UserProgress.MasteryState st = UserProgress.MasteryState.valueOf(c.getString(0));
                    map.put(st, c.getInt(1));
                } catch (Exception ignored) {}
            }
            c.close();
        }
        return map;
    }

    public double getChapterCompletion(int bab) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT count(*), SUM(CASE WHEN up.mastery_state != 'UNSEEN' THEN 1 ELSE 0 END) " +
                "FROM learning_objects lo " +
                "LEFT JOIN user_progress up ON lo.id = up.object_id " +
                "WHERE lo.type = 'VOCABULARY' AND lo.bab = ?",
                new String[]{String.valueOf(bab)}
        );
        double ratio = 0.0;
        if (c != null) {
            if (c.moveToFirst()) {
                int total = c.getInt(0);
                int done = c.getInt(1);
                if (total > 0) {
                    ratio = (double) done / (double) total;
                }
            }
            c.close();
        }
        return ratio;
    }

    public List<LearningObject> getRandomDistractors(LearningObject target, int count) {
        List<LearningObject> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query;
        String[] args;
        if (target.getType() == LearningObject.Type.VOCABULARY && target.getBab() != null) {
            query = "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = 'VOCABULARY' AND bab = ? AND id != ? ORDER BY RANDOM() LIMIT ?";
            args = new String[]{String.valueOf(target.getBab()), target.getId(), String.valueOf(count)};
        } else {
            query = "SELECT " + LO_COLUMNS + " FROM learning_objects WHERE type = ? AND id != ? ORDER BY RANDOM() LIMIT ?";
            args = new String[]{target.getType().name(), target.getId(), String.valueOf(count)};
        }
        Cursor c = db.rawQuery(query, args);
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToLearningObject(c));
            }
            c.close();
        }
        return list;
    }

    // --- Learning Cycle Persistence ---

    public void saveLearningCycle(LearningCycle cycle) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("cycle_id", cycle.getCycleId());
        cv.put("source", cycle.getSource().name());
        cv.put("material_type", cycle.getMaterialType().name());
        cv.put("configured_amount", cycle.getConfiguredAmount());
        cv.put("display_mode", cycle.getDisplayMode().name());
        cv.put("pool_definition_json", cycle.getPoolDefinitionJson());
        cv.put("started_at_epoch", cycle.getStartedAtEpoch());
        cv.put("completed_at_epoch", cycle.getCompletedAtEpoch());
        cv.put("status", cycle.getStatus().name());
        db.insertWithOnConflict("learning_cycles", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void saveCycleItem(CycleItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("cycle_id", item.getCycleId());
        cv.put("object_id", item.getObjectId());
        cv.put("pool_type", item.getPoolType().name());
        cv.put("cycle_iteration", item.getCycleIteration());
        cv.put("response", item.getResponse().name());
        cv.put("response_time_ms", item.getResponseTimeMs());
        cv.put("attempt_count", item.getAttemptCount());
        db.insert("cycle_items", null, cv);
    }

    public void completeCycle(String cycleId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", LearningCycle.Status.COMPLETED.name());
        cv.put("completed_at_epoch", System.currentTimeMillis() / 1000L);
        db.update("learning_cycles", cv, "cycle_id = ?", new String[]{cycleId});
    }

    public LearningCycle getActiveCycle() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT cycle_id, source, material_type, configured_amount, display_mode, pool_definition_json, started_at_epoch, completed_at_epoch, status " +
                "FROM learning_cycles WHERE status = 'ACTIVE' ORDER BY started_at_epoch DESC LIMIT 1",
                null
        );
        LearningCycle cycle = null;
        if (c != null) {
            if (c.moveToFirst()) {
                cycle = new LearningCycle(
                        c.getString(0),
                        LearningCycle.Source.valueOf(c.getString(1)),
                        LearningCycle.MaterialType.valueOf(c.getString(2)),
                        c.getInt(3),
                        LearningCycle.DisplayMode.valueOf(c.getString(4)),
                        c.getString(5),
                        c.getLong(6),
                        c.getLong(7),
                        LearningCycle.Status.valueOf(c.getString(8))
                );
            }
            c.close();
        }
        return cycle;
    }

    private LearningObject cursorToLearningObject(Cursor c) {
        String id = c.getString(0);
        LearningObject.Type type = LearningObject.Type.valueOf(c.getString(1));
        Integer bab = c.isNull(2) ? null : c.getInt(2);
        String level = c.getString(3);
        String japanese = c.getString(4);
        String reading = c.getString(5);
        String romaji = c.getString(6);
        String indonesian = c.getString(7);
        String badgeLabel = c.getString(8);
        String groupLabel = c.getString(9);
        int sortOrder = c.getInt(10);
        String detailsJson = c.getString(11);
        LearningObject.WordType wordType = LearningObject.WordType.LAINNYA;
        if (c.getColumnCount() > 12 && !c.isNull(12)) {
            wordType = LearningObject.WordType.fromString(c.getString(12));
        }
        return new LearningObject(id, type, wordType, bab, level, japanese, reading, romaji, indonesian, badgeLabel, groupLabel, sortOrder, detailsJson);
    }

    private UserProgress cursorToUserProgress(Cursor c) {
        String objectId = c.getString(0);
        int intervalDays = c.getInt(1);
        double easeFactor = c.getDouble(2);
        int repetitions = c.getInt(3);
        int lapses = c.getInt(4);
        double stability = c.getDouble(5);
        double retrievability = c.getDouble(6);
        int lastResponseTimeMs = c.getInt(7);
        long lastReviewEpoch = c.getLong(8);
        long nextReviewEpoch = c.getLong(9);
        UserProgress.MasteryState state;
        try {
            state = UserProgress.MasteryState.valueOf(c.getString(10));
        } catch (Exception e) {
            state = UserProgress.MasteryState.UNSEEN;
        }
        return new UserProgress(objectId, intervalDays, easeFactor, repetitions, lapses, stability, retrievability, lastResponseTimeMs, lastReviewEpoch, nextReviewEpoch, state);
    }
}
