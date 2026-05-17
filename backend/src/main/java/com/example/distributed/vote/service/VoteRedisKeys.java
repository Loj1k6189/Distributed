package com.example.distributed.vote.service;

final class VoteRedisKeys {

    private VoteRedisKeys() {
    }

    static String pollOptions(Long pollId) {
        return "vote:poll:" + pollId + ":options";
    }

    static String pollCounts(Long pollId) {
        return "vote:poll:" + pollId + ":counts";
    }

    static String redlock(String resource, int replicaIndex) {
        return "vote:redlock:" + replicaIndex + ":" + resource;
    }

    static String idempotent(String eventId) {
        return "vote:event:dedupe:" + eventId;
    }

    static String rateLimit(Long pollId, String dimension, String value) {
        return "vote:limit:" + pollId + ":" + dimension + ":" + value;
    }
}

