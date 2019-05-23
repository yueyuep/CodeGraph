package CProcess;

public class CAPIFunctionName {
    public static String[] mCWE119Funcs = new String[]{
            "cin", "getenv", "getenv_s", "wgetenv", "_wgetenv_s", "catgets", "gets", "getchar", "getc", "getch",
            "getche", "kbhit", "stdin", "getdlgtext", "getpass", "scanf", "fscanf", "vscanf", "vfscanf",
            "istream.get", "istream.getline", "istream.peek", "istream.read", "istream.putback",
            "streambuf.sbumpc", "streambuf.sgetc", "streambuf.sgetn", "streambuf.snextc", "streambuf.sputbackc",
            "SendMessage", "SendMessageCallback", "SendNotifyMessage", "PostMessage", "PostThreadMessage",
            "recv", "recvfrom", "Receive", "ReceiveFrom", "ReceiveFromEx", "Socket.Receive", "memcpy", "wmemcpy",
            "_memccpy", "memmove", "wmemmove", "memset", "wmemset", "memcmp", "wmemcmp", "memchr", "wmemchr",
            "strncpy", "_strncpy", "lstrcpyn", "_tcsncpy", "_mbsnbcpy", "_wcsncpy", "wcsncpy", "strncat",
            "_strncat", "_mbsncat", "wcsncat", "bcopy", "strcpy", "lstrcpy", "wcscpy", "_tcscpy", "_mbscpy",
            "CopyMemory", "strcat", "lstrcat", "lstrlen", "strchr", "strcmp", "strcoll", "strcspn", "strerror",
            "strlen", "strpbrk", "strrchr", "strspn", "strstr", "strtok", "strxfrm", "readlink", "fgets", "sscanf",
            "swscanf", "sscanf_s", "swscanf_s", "printf", "vprintf", "swprintf", "vsprintf", "asprintf", "vasprintf",
            "fprintf", "sprint", "snprintf", "_snprintf", "_snwprintf", "vsnprintf", "CString.Format", "CString.FormatV",
            "CString.FormatMessage", "CStringT.Format", "CStringT.FormatV", "CStringT.FormatMessage",
            "CStringT.FormatMessageV", "syslog", "malloc", "Winmain", "GetRawInput", "GetComboBoxInfo",
            "GetWindowText", "GetKeyNameText", "Dde", "GetFileMUI", "GetLocaleInfo", "GetString", "GetCursor",
            "GetScroll", "GetDlgItem", "GetMenuItem",
            "istream_get", "istream_getline", "istream_peek", "istream_read*", "istream_putback", "streambuf_sbumpc",
            "streambuf_sgetc", "streambuf_sgetn", "streambuf_snextc", "streambuf_sputbackc", "Socket_Receive*",
            "CString_Format", "CString_FormatV", "CString_FormatMessage", "CStringT_Format", "CStringT_FormatV",
            "CStringT_FormatMessage", "CStringT_FormatMessageV"
    };
    public static String[] mCWE399Funcs = new String[]{
            "free", "delete", "new", "malloc", "realloc", "calloc", "_alloca", "strdup", "asprintf", "vsprintf",
            "vasprintf", "sprintf", "snprintf", "_snprintf", "_snwprintf", "vsnprint"
    };

    public static boolean isCWE119Func(String funcName) {
        return isFuncNameIn(funcName, mCWE119Funcs);
    }

    public static boolean isCWE399Func(String funcName) {
        return isFuncNameIn(funcName, mCWE399Funcs);
    }

    public static boolean isFuncNameIn(String funcName, String[] funcs) {
        for (String name : funcs) {
            if (name.equals(funcName) || funcName.startsWith(name)) {
                return true;
            }
        }
        return false;
    }
}
