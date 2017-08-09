package com.six.dcsnodeManager;
/**   
* @author liusong  
* @date   2017年8月1日 
* @email  359852326@qq.com 
*/
public enum NodeEvent {
	/**丢失主节点事件**/
	MISS_MASTER,
	/**成为主节点事件**/
	BECOME_MASTER,
	/**丢失从节点事件**/
	MISS_SLAVE
}
