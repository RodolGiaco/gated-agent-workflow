package io.github.rodolgiaco.gaw;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns an issue title into the slug the runner puts in a branch name.
 *
 * <p>The rule is the one {@code kit/run-issue.sh} applies in shell: lowercase, then every run of
 * characters outside {@code a-z} and {@code 0-9} becomes a single hyphen, then no hyphen is left at
 * either end, and the result is cut to {@value #MAX_LENGTH} characters.
 */
public final class Slug {

  /** Longest slug the runner will put in a branch name. */
  public static final int MAX_LENGTH = 40;

  private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");

  private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");

  private Slug() {}

  /**
   * Derives the branch slug of an issue title.
   *
   * @param title the issue title
   * @return the slug, from one to {@value #MAX_LENGTH} characters long, with no hyphen at either
   *     end
   * @throws IllegalArgumentException if the title is null, or holds no character the slug can keep
   */
  public static String from(String title) {
    if (title == null) {
      throw new IllegalArgumentException("the issue title must not be null");
    }

    // Locale.ROOT, not the default locale: a Turkish default turns 'I' into a
    // dotless 'ı', which is outside a-z and would become a hyphen.
    String hyphenated = SEPARATORS.matcher(title.toLowerCase(Locale.ROOT)).replaceAll("-");
    String slug = EDGE_HYPHENS.matcher(hyphenated).replaceAll("");

    if (slug.length() > MAX_LENGTH) {
      // The cut can land right after a word and leave the hyphen that followed
      // it, so the trailing hyphen is dropped a second time.
      slug = EDGE_HYPHENS.matcher(slug.substring(0, MAX_LENGTH)).replaceAll("");
    }

    if (slug.isEmpty()) {
      throw new IllegalArgumentException("the issue title '" + title + "' produced an empty slug");
    }
    return slug;
  }
}
