package com.kotoba.app.engine;

import com.kotoba.app.data.model.CycleItem;
import com.kotoba.app.data.model.LearningCycle;
import com.kotoba.app.data.model.LearningObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ingat/Lupa Active-Recall Memory Loop Engine.
 * Implements short-term recovery loops for Kotoba, Kanji, Verbs, and Adjectives.
 * Invariant: Any item marked LUPA is buffered into a recovery pool that must be repeated
 * strictly until LUPA = 0 before the session can complete.
 */
public class IngatLupaEngine {
    private final LearningCycle mCycleConfig;
    private final List<LearningObject> mInitialPool;

    private List<LearningObject> mCurrentPassItems;
    private final List<LearningObject> mCurrentLupaList;
    private final List<LearningObject> mAllResolvedItems;
    private final List<CycleItem> mRecordedCycleItems;

    private int mCurrentIndex;
    private int mCycleIteration;
    private boolean mIsComplete;
    private int mTotalIngatCount;
    private int mTotalLupaCount;

    public IngatLupaEngine(LearningCycle config, List<LearningObject> pool) {
        if (config == null) {
            throw new IllegalArgumentException("LearningCycle config cannot be null");
        }
        this.mCycleConfig = config;
        this.mInitialPool = pool != null ? new ArrayList<>(pool) : new ArrayList<>();

        this.mCurrentPassItems = new ArrayList<>(mInitialPool);
        this.mCurrentLupaList = new ArrayList<>();
        this.mAllResolvedItems = new ArrayList<>();
        this.mRecordedCycleItems = new ArrayList<>();

        this.mCurrentIndex = 0;
        this.mCycleIteration = 1;
        this.mTotalIngatCount = 0;
        this.mTotalLupaCount = 0;
        this.mIsComplete = mInitialPool.isEmpty();
    }

    public LearningCycle getCycleConfig() {
        return mCycleConfig;
    }

    public boolean isComplete() {
        return mIsComplete;
    }

    public boolean isRecoveryRound() {
        return mCycleIteration > 1;
    }

    public int getCycleIteration() {
        return mCycleIteration;
    }

    public int getCurrentItemIndex() {
        return mCurrentIndex;
    }

    /**
     * Navigates backwards within the current pass.
     * Only moves the pointer — does NOT undo any committed Ingat/Lupa judgment.
     * Safe to call only with index >= 0 and < getCurrentPassTotal().
     */
    public void setCurrentItemIndex(int index) {
        if (index >= 0 && index < mCurrentPassItems.size()) {
            mCurrentIndex = index;
        }
    }

    public int getCurrentPassTotal() {
        return mCurrentPassItems.size();
    }

    public int getInitialPoolSize() {
        return mInitialPool.size();
    }

    public int getTotalIngatCount() {
        return mTotalIngatCount;
    }

    public int getTotalLupaCount() {
        return mTotalLupaCount;
    }

    public int getPendingLupaCount() {
        return mCurrentLupaList.size();
    }

    public List<CycleItem> getRecordedCycleItems() {
        return Collections.unmodifiableList(mRecordedCycleItems);
    }

    public LearningObject getCurrentItem() {
        if (mIsComplete || mCurrentIndex >= mCurrentPassItems.size()) {
            return null;
        }
        return mCurrentPassItems.get(mCurrentIndex);
    }

    /**
     * Mark the current item as remembered (INGAT).
     * @param responseTimeMs response latency in milliseconds
     * @return true if session completed on this answer, false if continuing
     */
    public boolean markIngat(int responseTimeMs) {
        if (mIsComplete) return true;
        LearningObject item = getCurrentItem();
        if (item == null) return true;

        mTotalIngatCount++;
        CycleItem.PoolType poolType = isRecoveryRound() ? CycleItem.PoolType.RECOVERY : CycleItem.PoolType.NORMAL;
        CycleItem cycleItem = new CycleItem(
                mCycleConfig.getCycleId(),
                item.getId(),
                poolType,
                mCycleIteration,
                CycleItem.Response.INGAT,
                responseTimeMs,
                1
        );
        mRecordedCycleItems.add(cycleItem);

        if (!mAllResolvedItems.contains(item)) {
            mAllResolvedItems.add(item);
        }

        mCurrentIndex++;
        checkEndOfPass();
        return mIsComplete;
    }

    /**
     * Mark the current item as forgotten (LUPA).
     * Buffers the item into mCurrentLupaList for the recovery loop.
     * @param responseTimeMs response latency in milliseconds
     * @return false (always requires at least one recovery round if Lupa was chosen)
     */
    public boolean markLupa(int responseTimeMs) {
        if (mIsComplete) return true;
        LearningObject item = getCurrentItem();
        if (item == null) return true;

        mTotalLupaCount++;
        CycleItem.PoolType poolType = isRecoveryRound() ? CycleItem.PoolType.RECOVERY : CycleItem.PoolType.NORMAL;
        CycleItem cycleItem = new CycleItem(
                mCycleConfig.getCycleId(),
                item.getId(),
                poolType,
                mCycleIteration,
                CycleItem.Response.LUPA,
                responseTimeMs,
                1
        );
        mRecordedCycleItems.add(cycleItem);

        // Add to recovery pool for the next iteration
        mCurrentLupaList.add(item);

        mCurrentIndex++;
        checkEndOfPass();
        return mIsComplete;
    }

    private void checkEndOfPass() {
        if (mCurrentIndex >= mCurrentPassItems.size()) {
            if (mCurrentLupaList.isEmpty()) {
                // All items in this pass were remembered!
                mIsComplete = true;
                mCycleConfig.setStatus(LearningCycle.Status.COMPLETED);
                mCycleConfig.setCompletedAtEpoch(System.currentTimeMillis() / 1000L);
            } else {
                // Recovery round needed: strictly repeat only the forgotten items
                mCurrentPassItems = new ArrayList<>(mCurrentLupaList);
                mCurrentLupaList.clear();
                mCycleIteration++;
                mCurrentIndex = 0;
            }
        }
    }
}
