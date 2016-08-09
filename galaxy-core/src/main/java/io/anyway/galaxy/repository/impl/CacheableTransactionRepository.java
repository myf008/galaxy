package io.anyway.galaxy.repository.impl;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.repository.TransactionRepository;

import java.sql.Connection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiong.j on 2016/7/21.
 */
public abstract class CacheableTransactionRepository implements TransactionRepository {

    private int expireDuration = 300;

    private Cache<Long, TransactionInfo> transactionInfoCache;

    @Override
    public int create(TransactionInfo transactionInfo) {
        int result = doCreate(transactionInfo);
        if (result > 0) {
            putToCache(transactionInfo);
        }
        return result;
    }

    @Override
    public int update(TransactionInfo transactionInfo) {
        int result = doUpdate(transactionInfo);
        if (result > 0) {
            putToCache(transactionInfo);
        } else {
            throw new ConcurrentModificationException();
        }
        return result;
    }

    @Override
    public int delete(TransactionInfo transactionInfo) {
        int result = doDelete(transactionInfo);
        if (result > 0) {
            removeFromCache(transactionInfo);
        }
        return result;
    }

    @Override
    public TransactionInfo findById(long txId) {
        TransactionInfo transactionInfo = findFromCache(txId);

        if (transactionInfo == null) {
            transactionInfo = doFindById(txId);

            if (transactionInfo != null) {
                putToCache(transactionInfo);
            }
        }

        return transactionInfo;
    }

    @Override
    public TransactionInfo lockById(long txId) {
        TransactionInfo transactionInfo = doLockById(txId);

        if (transactionInfo != null) {
            putToCache(transactionInfo);
        }

        return transactionInfo;
    }

    @Override
    public TransactionInfo directFindById( long txId) {
        TransactionInfo transactionInfo = doFindById(txId);

        if (transactionInfo != null) {
            putToCache(transactionInfo);
        }

        return transactionInfo;
    }

    @Override
    public List<TransactionInfo> find(TransactionInfo transactionInfo) {

        List<TransactionInfo> transactionInfos = doFind(transactionInfo);

//        for (TransactionInfo transactionInfo : transactionInfos) {
//            putToCache(transactionInfo);
//        }

        return transactionInfos;
    }

    @Override
    public List<TransactionInfo> findSince(java.sql.Date date, Integer[] txStatus) {

        List<TransactionInfo> transactionInfos = doFindSince(date, txStatus);

        return transactionInfos;
    }

    public CacheableTransactionRepository() {
        transactionInfoCache = CacheBuilder.newBuilder().expireAfterAccess(expireDuration, TimeUnit.SECONDS).maximumSize(1000).build();
    }

    protected void putToCache(TransactionInfo transactionInfo) {
        transactionInfoCache.put(transactionInfo.getTxId(), transactionInfo);
    }

    protected void removeFromCache(TransactionInfo transactionInfo) {
        transactionInfoCache.invalidate(transactionInfo.getTxId());
    }

    protected TransactionInfo findFromCache(long txId) {
        return transactionInfoCache.getIfPresent(txId);
    }

    public final void setExpireDuration(int durationInSeconds) {
        this.expireDuration = durationInSeconds;
    }

    protected abstract int doCreate( TransactionInfo transactionInfo);

    protected abstract int doUpdate(TransactionInfo transactionInfo);

    protected abstract int doDelete(TransactionInfo transactionInfo);

    protected abstract TransactionInfo doLockById( long txId);

    protected abstract List<TransactionInfo> doFind(TransactionInfo transactionInfo);

    protected abstract TransactionInfo doFindById(long txId);

    protected abstract List<TransactionInfo> doFindSince(java.sql.Date date, Integer[] txStatus);
}
