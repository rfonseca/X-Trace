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

#include "XtrContext.h"
#include <string.h>

namespace xtr {
    
Metadata Context::xtr_context;
bool Context::is_host_set = 0;
char Context::host_name[MAXHOSTNAME+1];

const Metadata& 
Context::getContext () 
{
    return xtr_context;
}


void 
Context::setContext (Metadata const& c)
{
    xtr_context = c;
}


void 
Context::unsetContext ()
{
    xtr_context.clear();
}    

void 
Context::forkContext ()
{
    xtr_context.newChainId();
}

void 
Context::logEvent(const char* agent, const char* label, u_int8_t severity )
{
    auto_ptr<Event> xte = createEvent(agent, label, severity);
    xte->sendReport();
}

auto_ptr<Event>
Context::createEvent( const char* agent, const char* label, u_int8_t severity)
{
    if (!is_host_set) 
        _set_host();
    auto_ptr<Event> xte(new Event());
    if (getContext().isValid()) {
        xte->addEdge(getContext());
    } else {
        Metadata xtr;
        xtr.setRandomTaskId();
        if (severity != OptionSeverity::_NONE)
            xte->setSeverity(severity & 0x7);
        xte->setTaskId(xtr.getTaskId());
    }
    if (is_host_set)
        xte->addInfo("Host", host_name);
    xte->addInfo("Agent", agent);
    xte->addInfo("Label", label);
    setContext(xte->getMetadata());
    return xte;
} 

void 
Context::setHost(const char* name) 
{
    strncpy(host_name, name, MAXHOSTNAME);
    host_name[MAXHOSTNAME] = '\0';
    is_host_set = true;
}

/* Private, tries to set the host automatically */
void 
Context::_set_host() 
{
    static bool tried = false;
    if (tried)
        return;
    tried = true;
    if (is_host_set)
        return;
    memset(host_name, 0, sizeof(host_name));
    if (!(gethostname (host_name, MAXHOSTNAME)) &&
        strchr(host_name, '.')) {
        is_host_set = true;
    }
    else {
        host_name[0] = '.'; host_name[1] = '\0';
    } 
}

};//namespace xtr