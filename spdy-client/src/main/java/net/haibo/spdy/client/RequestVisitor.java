/**
 * 
 */
package net.haibo.spdy.client;

/**
 * It can be used to extend the {@linkplain HttpRequest}' interface
 * without modify it.
 */
public interface RequestVisitor {
    void visit(HttpRequest r);
}
