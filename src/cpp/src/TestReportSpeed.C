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

#include <iostream>
#include <sys/time.h>
#include "Xtr.h"


using namespace std;
using namespace xtr;

double subtract(struct timeval *a, struct timeval *b) {
    long d = a->tv_sec - b->tv_sec;
    long ud = a->tv_usec - b->tv_usec;
    return d*1000000 + ud;
}

void report() {
    Context::logEvent("xte benchmark", "sample report");
}

void sampleReport() {
    Reporter::setSeverityThreshold(OptionSeverity::_NONE);
    auto_ptr<Event> xte = 
        Context::createEvent("xte benchmark", "sample report");
    cout << xte->getReport();
}

int main() {
    int rep = 100000;
    struct timeval t_start, t_end;
    double delta;
    Metadata x = Metadata::createRandom();
    Reporter::init();
    //Reporter::setSeverityThreshold(OptionSeverity::_NONE);
    Context::setContext(x);
    cout << "xte benchmark " << rep << "...";
    gettimeofday(&t_start, 0);
    for (int i = rep; i ;  i--) {
        report();
    }
    gettimeofday(&t_end, 0);
    delta = subtract(&t_end, &t_start);
    
    cout << "  " << delta <<  "us, time per call: " << delta/rep << "us, or " << (1000000.0/delta)*rep << "/s" << endl;
    sampleReport();
}
