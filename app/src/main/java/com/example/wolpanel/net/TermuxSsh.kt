package com.example.wolpanel.net

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

/**
 * Launches Termux to start an SSH session against [sshHost].
 *
 * Preferred path: dispatches `com.termux.RUN_COMMAND` so Termux auto-executes `ssh <host>`.
 * That requires the user to enable `allow-external-apps=true` in `~/.termux/termux.properties`.
 *
 * Fallback path: when RUN_COMMAND is unavailable (older Termux build, property not enabled,
 * background-start restriction), it copies `ssh <host>` to the clipboard and opens Termux so
 * the user can paste it.
 */
object TermuxSsh {

    private const val TERMUX_PKG = "com.termux"
    private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val SSH_BIN = "/data/data/com.termux/files/usr/bin/ssh"

    sealed class Result {
        object CommandDispatched : Result()
        object OpenedWithClipboard : Result()
        object TermuxNotInstalled : Result()
        data class Failed(val reason: String) : Result()
    }

    fun open(context: Context, sshHost: String): Result {
        val target = sshHost.trim()
        if (target.isEmpty()) return Result.Failed("SSH host is empty")
        if (!isTermuxInstalled(context)) return Result.TermuxNotInstalled

        runCommand(context, target)?.let { return it }
        return openWithClipboard(context, target)
    }

    private fun isTermuxInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(TERMUX_PKG, 0)
    }.isSuccess

    private fun runCommand(context: Context, target: String): Result? {
        val intent = Intent().apply {
            setClassName(TERMUX_PKG, RUN_COMMAND_SERVICE)
            action = RUN_COMMAND_ACTION
            putExtra("com.termux.RUN_COMMAND_PATH", SSH_BIN)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(target))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            // "0" = open the session in Termux's foreground UI, equivalent to a new terminal tab.
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        }
        return try {
            context.startService(intent)
            Result.CommandDispatched
        } catch (_: SecurityException) {
            // Thrown when `allow-external-apps=true` is missing in termux.properties.
            null
        } catch (_: IllegalStateException) {
            // Background service start blocked on newer Android — fall back to plain launch.
            null
        } catch (_: ActivityNotFoundException) {
            null
        }
    }

    private fun openWithClipboard(context: Context, target: String): Result {
        val cmd = "ssh $target"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("ssh command", cmd))

        val launch = context.packageManager.getLaunchIntentForPackage(TERMUX_PKG)
            ?: return Result.Failed("Could not launch Termux")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launch)
            Result.OpenedWithClipboard
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Failed to start Termux")
        }
    }
}
