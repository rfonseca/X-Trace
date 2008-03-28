/*
* Copyright (c) 2005,2006,2007 The Regents of the University of California.
* All rights reserved.
* 
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the University of California, nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
* 
* THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#include "XtrEvent.h"
#include <time.h>
#include <sys/time.h>
/* TODO:
   2. merge options, add options to the metadata
*/

namespace xtr {

Event::Event()
    : out_chain_ids(1), chain_id_index(0), timeset(false)
{
    info.reserve(2048);
    my_xtr.setRandomOpId();
}

Event::Event(const Metadata& xtr)
    : my_xtr(xtr.getTaskId(), xtr.getOpId()), 
      out_chain_ids(1), chain_id_index(0), timeset(false)
{
    info.reserve(2048);
    u_int8_t severity;
    if ((severity = xtr.getSeverity()) != OptionSeverity::DEFAULT)
        my_xtr.setSeverity(severity);
}

Event::Event(const Event& model)
    : out_chain_ids(1),
      chain_id_index(0), 
      info(model.info),
      timeset(false)
{
    my_xtr.setRandomOpId();
}

xtr_result
Event::setTaskId(const TaskId& taskId) 
{
    if (my_xtr.getTaskId().isValid() &&
        !my_xtr.getTaskId().isEqual(taskId))
        return XTR_FAIL;
    return my_xtr.setTaskId(taskId);
}

xtr_result
Event::setRandomOpId()
{
    my_xtr.setRandomOpId();
    return XTR_SUCCESS;
}

xtr_result
Event::setSeverity(u_int8_t s)
{
    return my_xtr.setSeverity(s);
}
void
Event::addTimestamp(const char* label) {
    //deprecated
}

void
Event::setTimestamp() {
    timeset = true;
    struct timezone *tz = 0;
    gettimeofday(&timestamp, tz);
}

//TODO: properly handle general options, as well as merging of
//      options for successive edges. 
xtr_result 
Event::addEdge(const Metadata& xtr, EventEdge::EdgeDir dir)
{
    u_int16_t chain_id;
    u_int8_t severity;

    //set the taskId or verifies that it is the same
    if (my_xtr.getTaskId().isValid() &&
        !my_xtr.getTaskId().isEqual(xtr.getTaskId())) {
        //trying to set a different taskId
        char buf[XTR_MD_MAX_LENGTH * 2 + 29];
        strncpy(buf, "Edge with different taskId: ", 29); 
        xtr.toString(buf + 28, sizeof(buf) - 28);
        addInfo("X-Trace-Error",buf);    
        return XTR_FAIL;
    }
    chain_id = xtr.getChainId();
    if (in_edges.size() == 0) {
        //this is the first edge
        my_xtr.setTaskId(xtr.getTaskId());
        //get the edge's chainId
        assert(out_chain_ids.size() == 1);
        out_chain_ids[0] = chain_id;  
        //set the severity, if not default
        severity = xtr.getSeverity();
        if (severity != OptionSeverity::DEFAULT) 
            my_xtr.setSeverity(severity);
    } 
    //add an edge to the context
    EventEdge e = EventEdge(xtr.getOpId(), dir, chain_id);
    in_edges.push_back(e);
    //mergeOptions(xtr); 
    return XTR_SUCCESS;
}

xtr_result 
Event::addInfo(const char* key, const char* value) 
{
    info.append(key);
    info.append(": ");
    info.append(value);
    info.append("\r\n");
    return XTR_SUCCESS;
}

/* Creates a new, random chain_id, adds it to the list of chain ids,
 * sets the current chain_id pointer to it, and returns the index of
 * the new chain_id.
 */
size_t 
Event::fork()
{
#if 0
    static bool random_init = 0;
    if (!random_init) { 
        srand((unsigned)time(0) ^ (u_int32_t)getpid() );
        random_init = 1;
    }
#endif
    u_int16_t id = (u_int16_t)(random() >> 16);
    out_chain_ids.push_back(id);
    return ++chain_id_index;
}

const Metadata& 
Event::getMetadata()
{
    if (my_xtr.getChainId() != out_chain_ids[chain_id_index])
        my_xtr.setChainId(out_chain_ids[chain_id_index]);
    return my_xtr;
}

const Metadata& 
Event::getMetadata(size_t index) 
{
    if (index >= out_chain_ids.size())
        index = 0;
    if (my_xtr.getChainId() != out_chain_ids[index])
        my_xtr.setChainId(out_chain_ids[index]);
    return my_xtr;
}

string 
Event::getReport() 
{
    char buf[1000];
    char buf2[10];
    char* p;
    size_t l,ll;
    Metadata xtr(my_xtr);
    //header and X-Trace
    string report;
    report.reserve(4096);
    report.append("X-Trace Report ver 1.0\r\nX-Trace: ");
    if (my_xtr.getChainId() != out_chain_ids[0])
        my_xtr.setChainId(out_chain_ids[0]);
    l = strlen(my_xtr.toString(buf, sizeof(buf))); 
    //Timestamp
    if (!timeset) //if it hasn't been set yet by any other call, we set it to now
       setTimestamp();
    ll = snprintf(buf + l, sizeof(buf) - l, 
                  "\r\nTimestamp: %ld.%03d", 
                  timestamp.tv_sec, 
                  (int)timestamp.tv_usec/1000);
    l += ll;
    //Severity
    u_int8_t severity;
    if ((severity = my_xtr.getSeverity()) != OptionSeverity::DEFAULT) {
        ll = snprintf(buf + l, sizeof(buf) - l, "\r\nSeverity: %d", severity);
        l += ll;
    }
    //chainId
    if (out_chain_ids[0]) {
        ll = snprintf(buf + l, sizeof(buf) - l, "\r\nChainId: 0x%04X", out_chain_ids[0]);
        //report << (buf);
    }
    report.append(buf);
    //edges
    p = buf;
    for (unsigned int i = 0; i < in_edges.size(); i++) {
        l = snprintf(buf, sizeof(buf), "\r\nEdge: %s, %s", 
               in_edges[i].opId.toString(buf2, sizeof(buf2)),
               EventEdge::DirName(in_edges[i].dir));
                               
        if (in_edges[i].chainId) {
            ll = snprintf(buf + l, sizeof(buf) - l, ", 0x%04X", in_edges[i].chainId);
            l += ll;
        }
        report.append(buf);
    }
    //info
    report.append("\r\n"); 
    report.append(info);
    return report;
}

xtr_result
Event::sendReport() 
{
    if (Reporter::willReport(my_xtr.getSeverity()))
        return Reporter::sendReport(getReport().c_str(), my_xtr.getSeverity());
    return XTR_FAIL;
}  

};

