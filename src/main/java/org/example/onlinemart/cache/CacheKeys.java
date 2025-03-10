package org.example.onlinemart.cache;

public final class CacheKeys {

    private CacheKeys() {
    }

    public static final class Orders {
        private static final String PREFIX = "orders:";

        public static String order(int orderId) {
            return PREFIX + "id:" + orderId;
        }

        public static final String ALL = PREFIX + "all";

        public static String userOrders(int userId) {
            return PREFIX + "user:" + userId;
        }

        public static String paginated(int page, int size) {
            return PREFIX + "page:" + page + ":size:" + size;
        }

        public static String processingLock(int orderId) {
            return PREFIX + "processing:lock:" + orderId;
        }
    }

    public static final class Products {
        private static final String PREFIX = "products:";

        public static String product(int productId) {
            return PREFIX + "id:" + productId;
        }

        public static final String ALL = PREFIX + "all";

        public static final String IN_STOCK = PREFIX + "instock";

        public static String category(String categoryName) {
            return PREFIX + "category:" + categoryName;
        }
    }

    public static final class Users {
        private static final String PREFIX = "users:";

        public static String user(int userId) {
            return PREFIX + "id:" + userId;
        }

        public static String username(String username) {
            return PREFIX + "username:" + username;
        }

        public static String email(String email) {
            return PREFIX + "email:" + email;
        }

        public static String watchlist(int userId) {
            return PREFIX + "watchlist:" + userId;
        }

        public static final String ALL = PREFIX + "all";
    }

    public static final class AdminSummary {
        private static final String PREFIX = "summary:";

        public static final String MOST_PROFITABLE = PREFIX + "mostProfit";

        public static final String TOTAL_SOLD = PREFIX + "totalSold";

        public static String topPopular(int count) {
            return PREFIX + "topPopular:" + count;
        }

        public static String salesByDateRange(String startDate, String endDate) {
            return PREFIX + "sales:" + startDate + ":" + endDate;
        }

        public static final String REVENUE_METRICS = PREFIX + "revenueMetrics";
    }

    public static final class UserActivity {
        private static final String PREFIX = "activity:";

        public static String frequentPurchases(int userId, int limit) {
            return PREFIX + "user:" + userId + ":frequent:" + limit;
        }

        public static String recentPurchases(int userId, int limit) {
            return PREFIX + "user:" + userId + ":recent:" + limit;
        }

        public static String rateLimit(String username, String endpoint) {
            return PREFIX + "rateLimit:" + username + ":" + endpoint;
        }
    }

    public static final class Session {
        private static final String PREFIX = "session:";

        public static String userSession(String sessionId) {
            return PREFIX + sessionId;
        }

        public static String userActiveSessions(int userId) {
            return PREFIX + "user:" + userId + ":active";
        }
    }

    public static final class System {
        private static final String PREFIX = "system:";

        public static final String CONFIG = PREFIX + "config";

        public static final String API_METRICS = PREFIX + "api:metrics";

        public static final String MAINTENANCE_MODE = PREFIX + "maintenance";
    }
}