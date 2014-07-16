/**
 * 
 */
package net.haibo.spdy.client;

/**
 * @author HAIBO
 *
 */
public interface IRequestVisitor {
    void visit(IHttpRequest r);
}
