/* Processor.java -- An annotation processor.
   Copyright (C) 2012, 2013  Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package javax.annotation.processing;

import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * <p>Provides the interface for an annotation processor.</p>
 * <p>Annotation processing is divided into a series of rounds,
 * with the input for each round being a subset of the output
 * from the previous round.  The input for the initial round
 * are those provided to the tool by the user, but, for the
 * purposes of operation, can be thought of as coming from a
 * virtual preceding round, as the behaviour is the same.
 * Once a processor is asked to process inputs in a particular
 * round, it is asked to do so in all subsequent rounds, even
 * if there are no annotations left for it to process.  These
 * inputs can include files generated by the tool's operation.</p>
 * <p>Annotation processors are found using a tool-specific
 * discovery method which may involve them being named directly
 * or discovered via a service-based lookup.  The processors which
 * are actually used are determined by which of the annotations present
 * on the root elements are supported by the processor and whether or
 * not it claims them.  Claimed annotation types are removed from
 * the set of unmatched annotations and are not passed to other
 * processors.  A round is complete once the set is empty or no
 * more processors are available.  If there are no annotation types
 * on the root elements (i.e. the set is empty to begin with), then
 * only <emph>universal processors</emph> (those which accept {@code "*"})
 * are run.</p>
 * <h2>Implementing an Annotation Processor</h2>
 * <p>The tool infrastructure expects the following from an annotation
 * processor:</p>
 * <ol>
 * <li>It should be able to create an instance of the processor using
 * a no-argument constructor and use this same instance for the whole run.</li>
 * <li>Once it has created the instance, it calls {@code init} with
 * an appropriate {@link ProcessingEnvironment}.</li>
 * <li>The tool calls {@link #getSupportedAnnotationTypes},
 * {@link #getSupportedOptions} and {@link #getSupportedSourceVersion} once
 * at the start of each run.</li>
 * <li>The tool calls {@link #process} on the processor for each round.</li>
 * </ol>
 * <p>Use outside the above protocol is undefined.  A processor supporting
 * {@code "*"} and returning {@code true} from the {@code process} method
 * will claim all annotations and prevent other processors from running.
 * If this is not the intention, then {@code false} should be returned.</p>
 * <p>To work well with different tool implementations, the processor
 * should make sure the following properties hold:</p>
 * <ol>
 * <li><strong>Orthogonality</strong>: The result of processing an input
 * is not dependent on the presence or not of other inputs.</li>
 * <li><strong>Consistency</strong>: Processing the same input should
 * always produce the same output.</li>
 * <li><strong>Commutativity</strong>: Processing input A then input B
 * should be the same as processing input B then input A.</li>
 * <li><strong>Independence</strong>: The result of processing an input
 * is not dependent on the presence of output from other processors.</li>
 * </ol>
 * <p>If a processor raises an error, this will be noted and the round
 * completed.  The subsequent round can then query as to whether an
 * error was raised using {@link RoundEnvironment#errorRaised()}.  If
 * an uncaught exception is raised, the tool may cease the use of other
 * annotation processors, so this should be used only in situations where
 * the usual error recovery process is infeasible.</p>
 * <p>The tool environment need not support annotation processors accessing
 * environmental resources, either per round or cross-round, in a multi-threaded
 * environment.  If a method returns {@code null} or other invalid input, or throws
 * an exception, in response to a configuration query, the tool may treat this
 * as an error condition.</p>
 * <p>The {@link Filer} interface documentation provides more information on how
 * files are handled and the restrictions in place.  Implementators may find it
 * easier to base their implementation on the {@link AbstractProcessor} class
 * rather than implementing this interface directly.</p>
 *
 * @author Andrew John Hughes (gnu_andrew@member.fsf.org)
 * @since 1.6
 */
public interface Processor
{

  /**
   * <p>Returns the names of the annotation types supported by this
   * processor.  These can take one of the following forms:</p>
   * <ol>
   * <li>A canonical fully-qualified type name.</li>
   * <li>A partial type name with the suffix {@code .*}</li>
   * <li>{@code .*}</li>
   * </ol>
   * <p>For example, {@code "gnu.classpath.annotations.ProcessMe"} matches
   * the specific annotation class, {@code ProcessMe}.  Alternatively,
   * {@code "gnu.classpath.annotations.*"} matches all annotations under
   * the package {@code gnu.classpath.annotations}.  Finally, {@code .*}
   * matches all annotations.</p>
   * <p>Processors should avoid claiming annotations they don't support,
   * as this may cause performance issues, and, if they also return
   * {@code true} when asked to {@link #process()} them, then they may
   * prevent other processors from handling those annotations.</p>
   *
   * @return the names of the supported annotation types.
   * @see SupportedAnnotationTypes
   */
  Set<String> getSupportedAnnotationTypes();

  /**
   * Returns the options supported by this tool.  Each string
   * returned by this method must be a period-separated sequence
   * of identifiers, as defined by
   * {@link SourceVersion#isIdentifier(CharSequence)}.
   * The tool may use this list to work out which options a user
   * provides are unsupported and print a warning.
   *
   * @return the names of the supported options or an empty set if none.
   * @see SupportedOptions
   */
  Set<String> getSupportedOptions();

  /**
   * Returns the latest source code version supported by this processor.
   *
   * @return the latest version supported.
   * @see SupportedSourceVersion
   * @see ProcessingEnvironment#getSourceVersion
   */
  SourceVersion getSupportedSourceVersion();

  /**
   * Initialises the processor with the specified environment.
   *
   * @param env the environment for the annotation processor, which gives
   *            it with access to facilities provided by the tool.
   */
  void init(ProcessingEnvironment env);

  /**
   * Processes a set of annotation types originating from the previous
   * round (including the virtual zeroth round formed from the user's input).
   * The processor may opt to claim these types by returning {@code true}.
   * If it does so, the types are claimed and they are not passed to
   * subsequent processors.  Whether a processor claims types or not may
   * vary, dependent on its own criteria.  A processor that supports
   * all types ({@code "*"}) is expected to gracefully handle the possibility
   * of receiving an empty set of annotations.
   *
   * @param types the annotations to process.
   * @param roundEnv information about this and the previous round.
   * @return true if the annotations are claimed by this processor, false otherwise.
   */
  boolean process(Set<? extends TypeElement> types, RoundEnvironment roundEnv);

  /**
   * <p>
   * Returns a series of suggested completions for an annotation or an empty
   * {@link Iterable}.  As a completion is being requested, the information
   * provided to the method may be incomplete, but either {@code element} or
   * {@code userText} must be non-{@code null}.  If the processor can not
   * provide any completions due to a lack of available information, it should
   * not throw a {@link NullPointerException}, but instead return an empty
   * {@code Iterable} or a single completion with an empty value string and
   * a message explaining why completions could not be returned.
   * </p>
   * <p>
   * Completions are aimed at annotation members where validity constraints reduce
   * the range of possible values.  For example, imagine an annotation {@code RainbowColour}
   * which takes as its value one of the following strings: {@code "RED"}, {@code "ORANGE"},
   * {@code "YELLOW"}, {@code "GREEN"}, {@code "BLUE"}, {@code "INDIGO"} and {@code "VIOLET"}.
   * If this annotation is passed as the annotation mirror argument to this method, then
   * a list of {@code Completion} objects may be returned as follows:</p>
   * <code>return Arrays.asList({@link Completions#of}("RED"), of("ORANGE"), of("YELLOW"),
   * of("GREEN"), of("BLUE"), of("INDIGO"), of("VIOLET"));</code>.  If {@code userText}
   * is set, it may be used to further reduce the range of possible values.  For example,
   * if {@code userText} was "R", then {@code Arrays.asList(of("RED"))} would be returned,
   * as only one possible value starts with {@code "R"}.  However, if {@code userText}
   * was {@code "P"}, then {@code Collections.emptyList()} would be returned, as there
   * are no possible values beginning with {@code "P"}.  Alternatively,
   * {@code of("", "No colours begin with the letter " + userText)} may be used to
   * give a more informative result.
   * </p>
   *
   * @param element the element being annotated.
   * @param annotation the annotation (possibly partial) being applied to the element.
   * @param member the annotation member to return completions for.
   * @param userText the source code text to be completed.
   * @return an {@code Iterable} over suggested completions to the annotation.
   */
  Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation,
						ExecutableElement member, String userText);
}

