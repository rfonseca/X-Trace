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

#ifndef _XTR_CONSTANTS_H
#define _XTR_CONSTANTS_H

#ifdef SOLARIS
#include <inttypes.h>
#define __ui(n) typedef uint## n ##_t u_int## n ##_t;
__ui(8)
__ui(16)
__ui(32)
#endif



enum {
    XTR_MD_MIN_LENGTH = 9,
    XTR_MD_MAX_LENGTH = 285,
    XTR_MAX_TASK_ID_LEN = 20,
    XTR_MAX_OP_ID_LEN   = 8,
};

typedef enum {
    XTR_SUCCESS,
    XTR_FAIL,
} xtr_result;

enum {
    XTR_MASK_HASOPTION = 0x04,
    XTR_MASK_VERSION   = 0xF0,
    XTR_MASK_OPIDLEN   = 0x08,
    XTR_MASK_IDLEN     = 0x03,
    XTR_CURRENT_VERSION   = 1,
};

enum {
    XTR_RPT_UDP_PORT = 7831,
    XTR_RPT_MAX_MSGSZ = 2048,
};

#endif
