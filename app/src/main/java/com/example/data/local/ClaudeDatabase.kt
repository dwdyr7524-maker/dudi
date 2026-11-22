package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ScheduledPromptEntity::class,
        TaskEntity::class,
        AppNotificationEntity::class,
        ActionLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ClaudeDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun controlCenterDao(): ControlCenterDao

    companion object {
        @Volatile
        private var INSTANCE: ClaudeDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE messages ADD COLUMN status TEXT NOT NULL DEFAULT 'SENT'"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scheduled_prompts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `promptText` TEXT NOT NULL,
                        `intervalHours` INTEGER NOT NULL,
                        `activeStartHour` INTEGER NOT NULL,
                        `activeEndHour` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `isPaused` INTEGER NOT NULL,
                        `targetConversationId` INTEGER NOT NULL,
                        `lastRunAt` INTEGER NOT NULL,
                        `nextRunAt` INTEGER NOT NULL,
                        `lastStatus` TEXT NOT NULL,
                        `lastExecutionKey` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `promptText` TEXT NOT NULL,
                        `dueAt` INTEGER NOT NULL,
                        `remindEnabled` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notification_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `relatedConversationId` INTEGER NOT NULL,
                        `relatedTaskId` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `action_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `inputTokens` INTEGER NOT NULL,
                        `outputTokens` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): ClaudeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClaudeDatabase::class.java,
                    "claude_classic.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
