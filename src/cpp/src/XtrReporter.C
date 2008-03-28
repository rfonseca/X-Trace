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

#include <sys/socket.h>
#include <arpa/inet.h>
#include <cstring>
#include <unistd.h>
#include <cstdio>
#include <cstdlib>
#include <cassert>
#include <cerrno>
#include "XtrReporter.h"

namespace xtr {

int Reporter::initialized;
int Reporter::report_socket;
struct sockaddr_in Reporter::dstaddr;
u_int8_t Reporter::severity_thresh;

void
Reporter::init() 
{
    if (initialized) return;
    severity_thresh = OptionSeverity::DEFAULT;
    if ((report_socket = socket(PF_INET, SOCK_DGRAM, 0)) < 0)
    {
        initialized = 0;
        return;
    }
    memset(&dstaddr, 0, sizeof(struct sockaddr_in));

    /* Configuration from the ENVIRONMENT */
    /* UDP Port */
    char port_s[7];
    char *port_env = getenv("XTR_FE_PORT");
    u_int16_t port = 0;
    if (port_env) {
        strncpy (port_s, port_env, 7);
        port_s[7] = '\0';
        port = (u_int16_t)(strtol(port_s, (char**)NULL, 0));
        if (errno == EINVAL || errno == ERANGE)
            port = 0;
    }
    if (port == 0)
        port = XTR_RPT_UDP_PORT;
    
    dstaddr.sin_family = AF_INET;
    dstaddr.sin_addr.s_addr = inet_addr("127.0.0.1");
    dstaddr.sin_port = htons(port);
    
    initialized = 1;
}

void
Reporter::stop() 
{
    initialized = 0;
    if (report_socket >= 0)
        if (close(report_socket) < 0)
            return;
}


xtr_result
Reporter::sendReport(const char *msg, u_int8_t severity)
{
    char buf[XTR_RPT_MAX_MSGSZ];

    if (!willReport(severity))
        return XTR_FAIL;

    // This check is done by will report
    //if (!initialized)
    //    return XTR_FAIL;
    if (!msg)
        return XTR_FAIL;
    if (report_socket < 0) {
        stop();
        return XTR_FAIL;
    } 

    strncpy(buf, msg, sizeof(buf));
    buf[sizeof(buf) - 1] = 0;

    if (sendto(report_socket, buf, strlen(buf), 0,
                (const struct sockaddr *) &dstaddr,
                sizeof(struct sockaddr_in)) < 0) {
        perror("sendto():");
        return XTR_FAIL;
    }
    return XTR_SUCCESS; 
}

xtr_result
Reporter::setSeverityThreshold(u_int8_t s) 
{
    if (!initialized)
        return XTR_FAIL;
    severity_thresh = s;
    return XTR_SUCCESS;
}

u_int8_t
Reporter::getSeverityThreshold()
{
    if (!initialized) 
        return OptionSeverity::_NONE;
    return severity_thresh;
}

bool
Reporter::willReport(u_int8_t s) 
{
    if (!initialized)
        return false;
    if (severity_thresh == OptionSeverity::_NONE)
        return false;
    return (s <= severity_thresh);
}

}; //namespace xtr
