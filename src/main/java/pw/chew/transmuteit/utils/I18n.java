/*
This file is originally from EssentialsX and has been modified for use in this plugin.
Original Source: https://github.com/EssentialsX/Essentials/blob/a09df9b/Essentials/src/com/earth2me/essentials/I18n.java
 */

package pw.chew.transmuteit.utils;

import org.bukkit.Bukkit;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class I18n {
    private static I18n instance;
    private static final String MESSAGES = "messages";
    private final transient Locale defaultLocale = Locale.getDefault();
    private transient Locale currentLocale = defaultLocale;
    private transient ResourceBundle customBundle;
    private transient ResourceBundle localeBundle;
    private final transient ResourceBundle defaultBundle;
    private transient Map<String, MessageFormat> messageFormatCache = new HashMap<>();
    private static final Pattern NODOUBLEMARK = Pattern.compile("''");
    private static final ResourceBundle NULL_BUNDLE = new ResourceBundle() {
        public Enumeration<String> getKeys() {
            return null;
        }

        protected Object handleGetObject(String key) {
            return null;
        }
    };

    public I18n() {
        defaultBundle = ResourceBundle.getBundle(MESSAGES, Locale.ENGLISH, new UTF8PropertiesControl());
        localeBundle = defaultBundle;
        customBundle = NULL_BUNDLE;
    }

    public void onEnable() {
        instance = this;
    }

    public void onDisable() {
        instance = null;
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    private String translate(final String string) {
        try {
            try {
                return customBundle.getString(string);
            } catch (MissingResourceException ex) {
                return localeBundle.getString(string);
            }
        } catch (MissingResourceException ex) {
            Logger.getLogger("Essentials").log(Level.WARNING, String.format("Missing translation key \"%s\" in translation file %s", ex.getKey(), localeBundle.getLocale().toString()), ex);
            return defaultBundle.getString(string);
        }
    }

    public static String tl(final String string, final Object... objects) {
        if (instance == null) {
            return "";
        }
        if (objects.length == 0) {
            return NODOUBLEMARK.matcher(instance.translate(string)).replaceAll("'");
        } else {
            return instance.format(string, objects);
        }
    }

    public String format(final String string, final Object... objects) {
        String format = translate(string);
        MessageFormat messageFormat = messageFormatCache.get(format);
        if (messageFormat == null) {
            try {
                messageFormat = new MessageFormat(format);
            } catch (IllegalArgumentException e) {
                format = format.replaceAll("\\{(\\D*?)\\}", "\\[$1\\]");
                messageFormat = new MessageFormat(format);
            }
            messageFormatCache.put(format, messageFormat);
        }
        return messageFormat.format(objects).replace(' ', ' '); // replace nbsp with a space
    }

    public void updateLocale(final String loc) {
        if (loc != null && !loc.isEmpty()) {
            final String[] parts = loc.split("[_\\.]");
            if (parts.length == 1) {
                currentLocale = new Locale(parts[0]);
            }
            if (parts.length == 2) {
                currentLocale = new Locale(parts[0], parts[1]);
            }
            if (parts.length == 3) {
                currentLocale = new Locale(parts[0], parts[1], parts[2]);
            }
        }
        ResourceBundle.clearCache();
        messageFormatCache = new HashMap<>();
        Logger.getLogger("TransmuteIt").log(Level.INFO, String.format("Using locale %s", currentLocale.toString()));

        try {
            localeBundle = ResourceBundle.getBundle(MESSAGES, currentLocale, new UTF8PropertiesControl());
        } catch (MissingResourceException ex) {
            localeBundle = NULL_BUNDLE;
        }

        try {
            customBundle = ResourceBundle.getBundle(MESSAGES, currentLocale, new FileResClassLoader(I18n.class.getClassLoader()), new UTF8PropertiesControl());
        } catch (MissingResourceException ex) {
            customBundle = NULL_BUNDLE;
        }
    }

    public static String capitalCase(final String input) {
        return input == null || input.length() == 0 ? input : input.toUpperCase(Locale.ENGLISH).charAt(0) + input.toLowerCase(Locale.ENGLISH).substring(1);
    }

    /**
     * Attempts to load properties files from the plugin directory before falling back to the jar.
     */
    private static class FileResClassLoader extends ClassLoader {
        private final transient File dataFolder;

        FileResClassLoader(final ClassLoader classLoader) {
            super(classLoader);
            this.dataFolder = Bukkit.getPluginManager().getPlugin("TransmuteIt").getDataFolder();
        }

        @Override
        public URL getResource(final String string) {
            final File file = new File(dataFolder, string);
            if (file.exists()) {
                try {
                    return file.toURI().toURL();
                } catch (MalformedURLException ignored) {}
            }
            return null;
        }

        @Override
        public InputStream getResourceAsStream(final String string) {
            final File file = new File(dataFolder, string);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException ignored) {}
            }
            return null;
        }
    }

    /**
     * Reads .properties files as UTF-8 instead of ISO-8859-1, which is the default on Java 8/below.
     * Java 9 fixes this by defaulting to UTF-8 for .properties files.
     */
    private static class UTF8PropertiesControl extends ResourceBundle.Control {
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IOException {
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    // use UTF-8 here, this is the important bit
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}